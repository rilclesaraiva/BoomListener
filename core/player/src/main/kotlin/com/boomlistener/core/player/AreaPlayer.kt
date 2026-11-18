// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Música que toca só enquanto uma área do app está aberta (o Wrapped): um player à
 * parte, que não mexe na fila nem no ponto da música do player principal. O que
 * estava tocando pausa ao [open] e volta ao [close], como na prévia do banner.
 */
class AreaPlayer(context: Context, private val controller: PlaybackController) {
    private val appContext = context.applicationContext
    private var player: ExoPlayer? = null
    private var resumeMain = false
    private var muted = false

    /** Pausa o player principal (guardando se ele tocava) e prepara o player da área. */
    fun open() {
        if (player != null) return
        resumeMain = controller.state.value.isPlaying
        if (resumeMain) controller.togglePlayPause()
        player = ExoPlayer.Builder(appContext)
            // Sem pedir o foco de áudio: o player principal já está pausado por nós
            // e, se perdesse o foco, não voltaria a tocar sozinho ao fechar.
            .setAudioAttributes(AudioAttributes.DEFAULT, false)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_ONE
                volume = if (muted) 0f else 1f
            }
    }

    /** Silencia a música da área (ou devolve o som); o player principal não é afetado. */
    fun setMuted(value: Boolean) {
        muted = value
        player?.volume = if (value) 0f else 1f
    }

    /** Toca [uri] em repetição (troca a que estava tocando). */
    fun play(uri: String) {
        val current = player ?: return
        current.setMediaItem(MediaItem.fromUri(uri))
        current.prepare()
        current.play()
    }

    /** App fora da tela: para de tocar sem perder o ponto. */
    fun pause() {
        player?.pause()
    }

    fun resume() {
        player?.play()
    }

    /** Libera o player da área e devolve o principal como estava. */
    fun close() {
        val current = player ?: return
        player = null
        current.release()
        if (resumeMain && !controller.state.value.isPlaying) controller.togglePlayPause()
        resumeMain = false
    }
}
