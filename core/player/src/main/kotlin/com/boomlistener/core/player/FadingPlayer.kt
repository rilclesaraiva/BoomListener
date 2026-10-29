// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.player

import android.os.Handler
import android.os.SystemClock
import androidx.media3.common.ForwardingPlayer
import java.util.concurrent.CopyOnWriteArraySet
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.FlagSet
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Volume final do player = volume da transição entre músicas × volume do
 * fade de tocar/pausar (e do timer para dormir). Cada um é um fator de 0 a 1.
 * As rampas são de volume de áudio (não animação de tela), feitas aqui.
 */
internal class VolumeMixer(private val exo: ExoPlayer, private val handler: Handler) {
    var transition = 1f
        set(value) {
            field = value
            apply()
        }
    /** Volume igualado pelo ReplayGain da música atual (1 = sem ajuste). */
    var replayGain = 1f
        set(value) {
            field = value
            apply()
        }
    private var fade = 1f
    private var ramp: Runnable? = null

    private fun apply() {
        exo.volume = (transition * fade * replayGain).coerceIn(0f, 1f)
    }

    /** Vai o fator de fade até [target] em [durationMs]; [then] roda no fim. */
    fun fadeTo(target: Float, durationMs: Long = PLAY_PAUSE_FADE_MS, then: () -> Unit = {}) {
        ramp?.let(handler::removeCallbacks)
        val from = fade
        val start = SystemClock.uptimeMillis()
        val step = object : Runnable {
            override fun run() {
                val t = ((SystemClock.uptimeMillis() - start).toFloat() / durationMs).coerceIn(0f, 1f)
                fade = from + (target - from) * t
                apply()
                if (t < 1f) {
                    handler.postDelayed(this, FRAME_MS)
                } else {
                    ramp = null
                    then()
                }
            }
        }
        ramp = step
        handler.post(step)
    }

    /** Sem rampa. */
    fun setFade(value: Float) {
        ramp?.let(handler::removeCallbacks)
        ramp = null
        fade = value
        apply()
    }

    companion object {
        const val PLAY_PAUSE_FADE_MS = 350L
        private const val FRAME_MS = 16L
    }
}

/**
 * O player que a sessão de mídia usa: igual ao ExoPlayer, mas tocar e pausar
 * (pelo app, notificação, fone ou tela de bloqueio) sobem/descem o volume
 * quando o fade está ligado nos Ajustes.
 */
internal class FadingPlayer(
    private val exo: ExoPlayer,
    private val mixer: VolumeMixer,
    private val fadeEnabled: () -> Boolean,
) : ForwardingPlayer(exo) {

    // Pausando com fade: o volume ainda está descendo, mas para quem pergunta
    // (a tela, a notificação) já está pausado.
    private var fadingOut = false

    // Quem escuta o player (a sessão de mídia). A sessão guarda o último estado
    // e só o atualiza quando chega um evento; como o ExoPlayer só pausa no fim
    // da descida, ela continuava achando "tocando" e reenviava isso à tela a
    // cada passo do volume: o botão ia e voltava (piscava). Por isso o aviso
    // de "pausei" (e de "voltou a tocar", se tocar no meio da descida) sai
    // daqui, na hora.
    private val listeners = CopyOnWriteArraySet<Player.Listener>()

    override fun addListener(listener: Player.Listener) {
        listeners += listener
        super.addListener(listener)
    }

    override fun removeListener(listener: Player.Listener) {
        listeners -= listener
        super.removeListener(listener)
    }

    override fun getPlayWhenReady(): Boolean = if (fadingOut) false else super.getPlayWhenReady()

    override fun isPlaying(): Boolean = if (fadingOut) false else super.isPlaying()

    override fun play() {
        if (fadingOut) {
            // Tocar no meio da descida: o ExoPlayer nem chegou a pausar, então
            // é só subir o volume de volta e avisar que voltou a tocar.
            fadingOut = false
            mixer.fadeTo(1f)
            announce(playing = true)
            return
        }
        if (fadeEnabled() && !exo.playWhenReady) {
            mixer.setFade(0f)
            super.play()
            mixer.fadeTo(1f)
        } else {
            mixer.setFade(1f)
            super.play()
        }
    }

    override fun pause() {
        if (fadeEnabled() && exo.isPlaying) {
            fadingOut = true
            announce(playing = false)
            mixer.fadeTo(0f) {
                fadingOut = false
                exo.pause()
                mixer.setFade(1f)
            }
        } else if (!fadingOut) {
            super.pause()
        }
    }

    @OptIn(UnstableApi::class)
    private fun announce(playing: Boolean) {
        val events = Player.Events(
            FlagSet.Builder()
                .add(Player.EVENT_PLAY_WHEN_READY_CHANGED)
                .add(Player.EVENT_IS_PLAYING_CHANGED)
                .build(),
        )
        for (listener in listeners) {
            listener.onPlayWhenReadyChanged(playing, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            listener.onIsPlayingChanged(playing)
            listener.onEvents(this, events)
        }
    }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        if (playWhenReady) play() else pause()
    }
}
