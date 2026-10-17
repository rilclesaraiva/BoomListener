// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.player

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
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

    override fun onCreate() {
        super.onCreate()
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

        val builder = MediaSession.Builder(this, player)
        openAppIntent()?.let { builder.setSessionActivity(it) }
        mediaSession = builder.build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    /** App removido dos recentes: se não estiver tocando, encerra o serviço. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
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
