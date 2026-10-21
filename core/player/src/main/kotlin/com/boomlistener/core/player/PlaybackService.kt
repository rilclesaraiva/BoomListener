// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.player

import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Serviço que toca a música de verdade. O Media3 cuida sozinho da notificação,
 * dos controles na tela de bloqueio, dos botões do fone Bluetooth e do Android Auto.
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val scope = MainScope()
    private lateinit var mixer: VolumeMixer
    private var exo: ExoPlayer? = null

    // Transição entre músicas: o volume acompanha o ponto da música (sobe no
    // começo e desce no fim) enquanto toca.
    private val transitionTick = object : Runnable {
        override fun run() {
            updateTransition()
            if (exo?.isPlaying == true) tickHandler.postDelayed(this, TRANSITION_TICK_MS)
        }
    }

    private fun updateTransition() {
        if (!::mixer.isInitialized) return
        val player = exo ?: return
        val seconds = PlaybackSettingsData.internalGet(this).prefs.value.transitionSeconds
        mixer.transition = transitionVolume(player.currentPosition, player.duration, seconds)
    }

    /**
     * Timer para dormir: na hora marcada, o som vai sumindo (3 s) e pausa; no
     * modo "fim da música", pausa quando a música atual acaba.
     */
    private suspend fun runSleepTimer(timer: SleepTimer?) {
        val player = exo ?: return
        player.pauseAtEndOfMediaItems = timer?.endOfTrack == true
        val endsAt = timer?.endsAtMs ?: return
        delay((endsAt - System.currentTimeMillis()).coerceAtLeast(0))
        mixer.fadeTo(0f, SLEEP_FADE_MS) {
            player.pause()
            mixer.setFade(1f)
        }
        PlaybackSettingsData.internalGet(this).cancelSleepTimer()
    }

    // "Mais ouvidas": decide o que conta (PlayCountTracker) e avisa o app.
    private val playCounter = PlayCountTracker()
    private val tickHandler = Handler(Looper.getMainLooper())

    // Lembrar onde parou: a fila e o ponto da música vão para um arquivo.
    private lateinit var snapshots: PlaybackSnapshotStore
    private var ticksSinceSave = 0
    private val saveSoon = Runnable { saveSnapshot() }

    private val tick = object : Runnable {
        override fun run() {
            reportIfCounted()
            // Tocando: guarda o ponto da música a cada ~5 s.
            if (++ticksSinceSave >= 5) {
                ticksSinceSave = 0
                saveSnapshot()
            }
            if (mediaSession?.player?.isPlaying == true) tickHandler.postDelayed(this, 1_000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        snapshots = PlaybackSnapshotStore(this)
        val player = ExoPlayer.Builder(this)
            // "true": pausa quando outro app toca som (ligação, vídeo…) e volta depois.
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            // Pausa quando o fone é desconectado.
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                // Ordem de embaralhamento neutra: o app embaralha a fila ele
                // mesmo (ShuffleState), então o player nunca reordena nada.
                setShuffleOrder(ShuffleOrder.UnshuffledShuffleOrder(0))
            }

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val item = mediaItem ?: return
                playCounter.start(
                    key = item.mediaId,
                    durationMs = player.duration.takeIf { it > 0 } ?: 0L,
                    chosen = ChosenSongSignal.consume(item.mediaId),
                    shuffle = player.shuffleModeEnabled,
                    isPlaying = player.isPlaying,
                    nowMs = SystemClock.elapsedRealtime(),
                )
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                // Timer "fim da música": pausou sozinho no fim da música, missão cumprida.
                if (!playWhenReady && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) {
                    PlaybackSettingsData.internalGet(this@PlaybackService).cancelSleepTimer()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                tickHandler.removeCallbacks(transitionTick)
                if (isPlaying) tickHandler.post(transitionTick)
                playCounter.setPlaying(isPlaying, SystemClock.elapsedRealtime())
                tickHandler.removeCallbacks(tick)
                if (isPlaying) tickHandler.post(tick) else reportIfCounted()
                scheduleSave()
            }

            override fun onEvents(player: Player, events: Player.Events) {
                // Fila, música atual ou repetição mudaram: guarda (agrupando mudanças seguidas).
                if (events.containsAny(
                        Player.EVENT_TIMELINE_CHANGED,
                        Player.EVENT_MEDIA_ITEM_TRANSITION,
                        Player.EVENT_REPEAT_MODE_CHANGED,
                        Player.EVENT_POSITION_DISCONTINUITY,
                    )
                ) {
                    scheduleSave()
                }
            }
        })

        // Abriu o app de novo: volta a fila e o ponto onde parou, pausado.
        snapshots.read()?.let { restore(player, it) }

        // Ajustes de reprodução: fade ao tocar/pausar, transição entre músicas,
        // velocidade e timer para dormir.
        val settings = PlaybackSettingsData.internalGet(this)
        mixer = VolumeMixer(player, tickHandler)
        val sessionPlayer = FadingPlayer(player, mixer) { settings.prefs.value.fadeOnPlayPause }
        exo = player
        scope.launch {
            settings.prefs.collect { prefs ->
                player.setPlaybackSpeed(prefs.speed)
                updateTransition()
            }
        }
        scope.launch { settings.sleepTimer.collectLatest { timer -> runSleepTimer(timer) } }

        val builder = MediaSession.Builder(this, sessionPlayer)
        openAppIntent()?.let { builder.setSessionActivity(it) }
        mediaSession = builder.build()

        // Saída de som escolhida na tela "Dispositivos" (null = o sistema decide).
        val outputs = AudioOutputsData.internalGet(this)
        // collectLatest + espera curta: trocar várias vezes seguidas aplica só a
        // última escolha, e sempre com o aparelho lido na hora (o objeto antigo
        // pode não valer mais depois que o Android refaz as rotas).
        scope.launch {
            outputs.preferred.collectLatest { id ->
                delay(OUTPUT_SWITCH_DELAY_MS)
                player.setPreferredAudioDevice(id?.let(outputs::deviceInfo))
            }
        }
    }

    private fun reportIfCounted() {
        val player = mediaSession?.player ?: return
        val now = SystemClock.elapsedRealtime()
        // Histórico (toda música ouvida 30 s) e "mais ouvidas" (com as regras do aleatório).
        playCounter.checkListened(now)?.let { key -> playOf(player, key)?.let { PlaybackHooks.onListened?.invoke(it) } }
        playCounter.check(now)?.let { key -> playOf(player, key)?.let { PlaybackHooks.onPlayCounted?.invoke(it) } }
    }

    private fun playOf(player: Player, key: String): CountedPlay? {
        val item = player.currentMediaItem?.takeIf { it.mediaId == key } ?: return null
        val metadata = item.mediaMetadata
        return CountedPlay(
            uri = key,
            title = metadata.title?.toString().orEmpty(),
            artist = metadata.artist?.toString(),
            album = metadata.albumTitle?.toString(),
            timestampMs = System.currentTimeMillis(),
        )
    }

    private fun restore(player: ExoPlayer, snapshot: PlaybackSnapshot) {
        val items = snapshot.items.map { item ->
            MediaItem.Builder()
                .setMediaId(item.uri)
                .setUri(item.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(item.title)
                        .setArtist(item.artist)
                        .setAlbumTitle(item.album)
                        .setIsPlayable(true)
                        .setIsBrowsable(false)
                        .build(),
                )
                .build()
        }
        player.setMediaItems(items, snapshot.index, snapshot.positionMs)
        player.repeatMode = snapshot.repeatMode
        player.playWhenReady = false
        player.prepare()
    }

    private fun scheduleSave() {
        tickHandler.removeCallbacks(saveSoon)
        tickHandler.postDelayed(saveSoon, 500)
    }

    /** Fila vazia (música encerrada) apaga o que estava guardado: o app volta limpo. */
    private fun saveSnapshot() {
        val player = mediaSession?.player ?: return
        val count = player.mediaItemCount
        if (count == 0) {
            snapshots.save(null)
            return
        }
        val items = (0 until count).map { index ->
            val item = player.getMediaItemAt(index)
            SnapshotItem(
                uri = item.mediaId,
                title = item.mediaMetadata.title?.toString().orEmpty(),
                artist = item.mediaMetadata.artist?.toString(),
                album = item.mediaMetadata.albumTitle?.toString(),
            )
        }
        snapshots.save(
            PlaybackSnapshot(
                items = items,
                index = player.currentMediaItemIndex.coerceIn(0, count - 1),
                positionMs = player.currentPosition.coerceAtLeast(0),
                repeatMode = player.repeatMode,
            ),
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    /** App removido dos recentes: se não estiver tocando, encerra o serviço. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        saveSnapshot()
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        tickHandler.removeCallbacks(transitionTick)
        tickHandler.removeCallbacks(tick)
        tickHandler.removeCallbacks(saveSoon)
        saveSnapshot()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    /** Tocar na notificação abre o app (sem este módulo conhecer o :app). */
    private fun openAppIntent(): PendingIntent? {
        val launch = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        return PendingIntent.getActivity(
            this,
            0,
            launch,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}

/** Espera antes de aplicar a saída escolhida (junta toques seguidos). */
private const val OUTPUT_SWITCH_DELAY_MS = 250L

/** De quanto em quanto tempo o volume da transição é atualizado. */
private const val TRANSITION_TICK_MS = 50L

/** O timer para dormir abaixa o som devagar antes de pausar. */
private const val SLEEP_FADE_MS = 3_000L
