// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.player

import android.os.Handler
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import com.boomlistener.core.common.DiagnosticLog
import java.util.Locale

/**
 * Anota no registro de diagnóstico (só se a pessoa ligou) o que o player faz: estado,
 * saltos de posição (seek), erros de áudio, troca de itens da fila e, a cada poucos
 * segundos tocando, a posição e o volume (com cada parte dele). Serve para achar bugs como
 * "a música ficou muda depois de gravar tags". Nunca anota título, artista nem endereço.
 */
@OptIn(UnstableApi::class)
internal class PlaybackDiagnostics(
    private val exo: ExoPlayer,
    /** As partes do volume (transição, fade, ReplayGain), para o registro. */
    private val volumeParts: () -> String,
) {
    private val handler = Handler(Looper.getMainLooper())

    private val snapshot = object : Runnable {
        override fun run() {
            if (!exo.isPlaying) return
            log("tocando: ${where()} ${volumeText()}")
            handler.postDelayed(this, SNAPSHOT_MS)
        }
    }

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            log("estado=${stateName(playbackState)} ${where()} ${volumeText()}")
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            log("tocando=$isPlaying ${where()} ${volumeText()}")
            handler.removeCallbacks(snapshot)
            if (isPlaying) handler.postDelayed(snapshot, SNAPSHOT_MS)
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            log("quer tocar=$playWhenReady (${playWhenReadyReason(reason)})")
        }

        override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {
            log("supressão de reprodução=$playbackSuppressionReason")
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            log(
                "salto (${discontinuityName(reason)}): ${secs(oldPosition.positionMs)} → ${secs(newPosition.positionMs)}, " +
                    "item ${oldPosition.mediaItemIndex} → ${newPosition.mediaItemIndex}",
            )
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            val why = if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) "fila mudou" else "fonte atualizada"
            log("linha do tempo ($why): ${timeline.windowCount} itens, ${where()}")
        }

        override fun onTracksChanged(tracks: Tracks) {
            val audio = tracks.groups
                .filter { it.type == C.TRACK_TYPE_AUDIO && it.isSelected }
                .flatMap { group -> (0 until group.length).map { group.getTrackFormat(it) } }
                .joinToString { "${it.sampleMimeType} ${it.sampleRate} Hz ${it.averageBitrate / 1000} kbps" }
            log("faixas de áudio: ${audio.ifEmpty { "nenhuma" }}")
        }

        override fun onPlayerError(error: PlaybackException) {
            log("ERRO do player: ${error.errorCodeName} (${error.cause?.javaClass?.simpleName}) ${where()}")
        }
    }

    private val analytics = object : AnalyticsListener {
        override fun onAudioSinkError(eventTime: AnalyticsListener.EventTime, audioSinkError: Exception) {
            log("ERRO na saída de áudio: ${audioSinkError.javaClass.simpleName}")
        }

        override fun onAudioCodecError(eventTime: AnalyticsListener.EventTime, audioCodecError: Exception) {
            log("ERRO no decodificador de áudio: ${audioCodecError.javaClass.simpleName}")
        }

        override fun onAudioUnderrun(
            eventTime: AnalyticsListener.EventTime,
            bufferSize: Int,
            bufferSizeMs: Long,
            elapsedSinceLastFeedMs: Long,
        ) {
            log("áudio sem dados (underrun): ${elapsedSinceLastFeedMs} ms sem alimentar a saída")
        }
    }

    fun attach() {
        exo.addListener(listener)
        exo.addAnalyticsListener(analytics)
    }

    private fun where() = "pos=${secs(exo.currentPosition)}/${secs(exo.duration)}"

    private fun volumeText() = "volume=${"%.2f".format(Locale.ROOT, exo.volume)} (${volumeParts()})"

    private fun log(message: String) = DiagnosticLog.log("player", message)

    private fun secs(ms: Long): String = if (ms == C.TIME_UNSET) "?" else "%.1fs".format(Locale.ROOT, ms / 1000.0)

    private fun stateName(state: Int) = when (state) {
        Player.STATE_IDLE -> "parado"
        Player.STATE_BUFFERING -> "carregando"
        Player.STATE_READY -> "pronto"
        Player.STATE_ENDED -> "terminou"
        else -> state.toString()
    }

    private fun playWhenReadyReason(reason: Int) = when (reason) {
        Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST -> "pedido"
        Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS -> "perdeu o foco de áudio"
        Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY -> "fone saiu"
        Player.PLAY_WHEN_READY_CHANGE_REASON_REMOTE -> "controle remoto"
        Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM -> "fim da música"
        else -> "motivo $reason"
    }

    private fun discontinuityName(reason: Int) = when (reason) {
        Player.DISCONTINUITY_REASON_AUTO_TRANSITION -> "próxima música"
        Player.DISCONTINUITY_REASON_SEEK -> "seek"
        Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT -> "ajuste do seek"
        Player.DISCONTINUITY_REASON_SKIP -> "pulou"
        Player.DISCONTINUITY_REASON_REMOVE -> "item removido"
        Player.DISCONTINUITY_REASON_INTERNAL -> "interno"
        else -> "motivo $reason"
    }

    private companion object {
        const val SNAPSHOT_MS = 3_000L
    }
}
