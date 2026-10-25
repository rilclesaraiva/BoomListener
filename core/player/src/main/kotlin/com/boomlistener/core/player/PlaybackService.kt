// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.player

import android.app.PendingIntent
import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.Tracks
import androidx.media3.extractor.metadata.id3.InternalFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
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
    /** Ganho ReplayGain (dB) da música atual, lido das tags; null = sem tag. */
    private var trackGainDb: Float? = null
    /** Quando a música pausou porque o fone saiu (null = não foi isso). */
    private var pausedByHeadphonesAt: Long? = null
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
        // MP3 e AAC (.aac) sem índice: pular para um ponto da música usa a taxa
        // de bits constante (sem isso, arrastar a barra caía no lugar errado ou nem ia).
        val extractors = DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true)
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this, extractors))
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
            // Arquivo que o aparelho não toca (WMA, AIFF, DSD, ALAC…): pula para a
            // próxima, em vez de parar tudo. O aviso na tela vem do controller.
            override fun onPlayerError(error: PlaybackException) {
                if (error.errorCode !in UNSUPPORTED_FORMAT_ERRORS) return
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem()
                    player.prepare()
                }
            }

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

            // ReplayGain: as tags da música nova chegam com as faixas dela.
            override fun onTracksChanged(tracks: Tracks) {
                trackGainDb = replayGainDb(gainTagsOf(tracks))
                applyReplayGain()
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                // Pausou porque o fone saiu: lembra, para voltar a tocar quando ele reconectar.
                pausedByHeadphonesAt = if (!playWhenReady && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY) {
                    SystemClock.elapsedRealtime()
                } else {
                    null
                }
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
                applyReplayGain()
            }
        }
        scope.launch { settings.sleepTimer.collectLatest { timer -> runSleepTimer(timer) } }
        getSystemService(AudioManager::class.java)?.registerAudioDeviceCallback(headphonesCallback, tickHandler)

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
        val items = snapshot.items.map(::snapshotMediaItem)
        player.setMediaItems(items, snapshot.index, snapshot.positionMs)
        player.repeatMode = snapshot.repeatMode
        // "Lembrar o aleatório": a fila já está embaralhada; volta ligado, com a ordem original guardada.
        val original = snapshot.original
        if (snapshot.shuffle && original != null && PlaybackSettingsData.internalGet(this).prefs.value.keepShuffle) {
            SharedShuffle.state.adopt(original.map(::snapshotMediaItem))
            player.shuffleModeEnabled = true
        }
        player.playWhenReady = false
        player.prepare()
    }

    private fun snapshotMediaItem(item: SnapshotItem): MediaItem =
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

    private fun MediaItem.toSnapshotItem() = SnapshotItem(
        uri = mediaId,
        title = mediaMetadata.title?.toString().orEmpty(),
        artist = mediaMetadata.artist?.toString(),
        album = mediaMetadata.albumTitle?.toString(),
    )

    /** Volume igualado pelo ReplayGain (só com a opção ligada). */
    private fun applyReplayGain() {
        if (!::mixer.isInitialized) return
        val on = PlaybackSettingsData.internalGet(this).prefs.value.replayGain
        mixer.replayGain = if (on) replayGainVolume(trackGainDb) else 1f
    }

    /** As tags de ganho da faixa de áudio que está tocando (ID3, Vorbis/FLAC/Opus e MP4). */
    private fun gainTagsOf(tracks: Tracks): List<Pair<String, String>> = buildList {
        for (group in tracks.groups) {
            if (group.type != C.TRACK_TYPE_AUDIO || !group.isSelected) continue
            for (i in 0 until group.length) {
                val metadata = group.getTrackFormat(i).metadata ?: continue
                for (e in 0 until metadata.length()) {
                    when (val entry = metadata.get(e)) {
                        is TextInformationFrame -> {
                            val name = entry.description ?: continue
                            entry.values.firstOrNull()?.let { add(name to it) }
                        }
                        is VorbisComment -> add(entry.key to entry.value)
                        is InternalFrame -> add(entry.description to entry.text)
                    }
                }
            }
        }
    }

    /**
     * Fone reconectado: se a música pausou porque ele saiu (há pouco) e a
     * opção está ligada, volta a tocar. Sem permissão de Bluetooth: o
     * AudioManager avisa das saídas de som novas.
     */
    private val headphonesCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            val pausedAt = pausedByHeadphonesAt ?: return
            if (!PlaybackSettingsData.internalGet(this@PlaybackService).prefs.value.resumeOnReconnect) return
            if (SystemClock.elapsedRealtime() - pausedAt > RESUME_WINDOW_MS) return
            if (addedDevices.none { it.isSink && it.type in HEADPHONE_TYPES }) return
            pausedByHeadphonesAt = null
            mediaSession?.player?.play()
        }
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
        val items = (0 until count).map { index -> player.getMediaItemAt(index).toSnapshotItem() }
        snapshots.save(
            PlaybackSnapshot(
                items = items,
                index = player.currentMediaItemIndex.coerceIn(0, count - 1),
                positionMs = player.currentPosition.coerceAtLeast(0),
                repeatMode = player.repeatMode,
                shuffle = player.shuffleModeEnabled,
                original = SharedShuffle.state.originalItems()?.takeIf { player.shuffleModeEnabled }?.map { it.toSnapshotItem() },
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
        getSystemService(AudioManager::class.java)?.unregisterAudioDeviceCallback(headphonesCallback)
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

/** Até quanto tempo depois de o fone sair a música volta sozinha quando ele reconecta. */
private const val RESUME_WINDOW_MS = 30 * 60_000L

/** Saídas que contam como fone (com fio, USB, Bluetooth). */
private val HEADPHONE_TYPES: Set<Int> = buildSet {
    add(AudioDeviceInfo.TYPE_WIRED_HEADSET)
    add(AudioDeviceInfo.TYPE_WIRED_HEADPHONES)
    add(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) add(AudioDeviceInfo.TYPE_USB_HEADSET)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(AudioDeviceInfo.TYPE_BLE_HEADSET)
}

/** De quanto em quanto tempo o volume da transição é atualizado. */
private const val TRANSITION_TICK_MS = 50L

/** O timer para dormir abaixa o som devagar antes de pausar. */
private const val SLEEP_FADE_MS = 3_000L
