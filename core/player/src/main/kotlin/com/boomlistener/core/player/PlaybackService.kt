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

/**
 * Serviço que toca a música de verdade. O Media3 cuida sozinho da notificação,
 * dos controles na tela de bloqueio, dos botões do fone Bluetooth e do Android Auto.
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

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
                reportNowPlaying()
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

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playCounter.setPlaying(isPlaying, SystemClock.elapsedRealtime())
                tickHandler.removeCallbacks(tick)
                if (isPlaying) tickHandler.post(tick) else reportIfCounted()
                scheduleSave()
                reportNowPlaying()
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

        val builder = MediaSession.Builder(this, player)
        openAppIntent()?.let { builder.setSessionActivity(it) }
        mediaSession = builder.build()
    }

    private fun reportIfCounted() {
        val player = mediaSession?.player ?: return
        val now = SystemClock.elapsedRealtime()
        // Histórico (toda música ouvida 30 s) e "mais ouvidas" (com as regras do aleatório).
        playCounter.checkListened(now)?.let { key -> playOf(player, key)?.let { PlaybackHooks.onListened?.invoke(it) } }
        playCounter.check(now)?.let { key -> playOf(player, key)?.let { PlaybackHooks.onPlayCounted?.invoke(it) } }
    }

    private fun reportNowPlaying() {
        val player = mediaSession?.player ?: return
        val current = player.currentMediaItem
        val play = if (player.isPlaying && current != null) playOf(player, current.mediaId) else null
        PlaybackHooks.onNowPlaying?.invoke(play)
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
