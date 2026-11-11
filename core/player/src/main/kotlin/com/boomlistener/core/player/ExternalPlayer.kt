// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** O que o player pequeno do "Abrir com" mostra. */
data class ExternalPlayerState(
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
) {
    val progress: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

/**
 * Player próprio do pop-up de "Abrir com": toca um arquivo de fora sem encostar
 * na fila nem na música do player do app (que fica como estava, só pausa se o
 * Android tirar o foco de áudio dele). Vive enquanto o pop-up existe; sem
 * notificação nem controle de mídia. Usar só na thread principal.
 */
class ExternalPlayer(context: Context) {
    private val player = ExoPlayer.Builder(context.applicationContext)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            /* handleAudioFocus = */ true,
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    private var released = false

    private val mutableState = MutableStateFlow(ExternalPlayerState())
    val state: StateFlow<ExternalPlayerState> = mutableState.asStateFlow()

    init {
        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) = refresh()
        })
    }

    fun play(uri: String) {
        if (released) return
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.play()
    }

    fun togglePlayPause() {
        if (released) return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekToFraction(fraction: Float) {
        if (released) return
        val duration = player.duration
        if (duration != C.TIME_UNSET && duration > 0) player.seekTo((duration * fraction.coerceIn(0f, 1f)).toLong())
        refresh()
    }

    /** Atualiza a posição (a tela chama a cada meio segundo enquanto toca). */
    fun refresh() {
        if (released) return
        val duration = player.duration.takeIf { it != C.TIME_UNSET } ?: 0L
        mutableState.value = ExternalPlayerState(
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0),
            durationMs = duration,
        )
    }

    fun release() {
        if (released) return
        released = true
        player.release()
    }
}
