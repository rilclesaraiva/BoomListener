// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink

/*
 * "Sem pausa entre faixas" (gapless) desligável. O ExoPlayer corta sozinho o
 * silêncio que o codificador põe no começo e no fim de MP3/AAC (encoderDelay
 * e encoderPadding, lidos do arquivo). Desligado, o app zera esses números
 * antes de o som chegar à saída: o silêncio fica, como num player comum.
 */

/** Fábrica de renderizadores com a saída de som que respeita o ajuste de gapless. */
@OptIn(UnstableApi::class)
internal class BoomRenderersFactory(
    context: Context,
    private val gapless: () -> Boolean,
) : DefaultRenderersFactory(context) {
    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioOutputPlaybackParams: Boolean,
    ): AudioSink? = super.buildAudioSink(context, enableFloatOutput, enableAudioOutputPlaybackParams)
        ?.let { GaplessToggleSink(it, gapless) }
}

/** Vale a partir da próxima música (o ajuste é lido a cada formato novo). */
@OptIn(UnstableApi::class)
private class GaplessToggleSink(sink: AudioSink, private val gapless: () -> Boolean) : ForwardingAudioSink(sink) {
    @Suppress("DEPRECATION") // preferredBufferSizeOverride ainda é repassado como veio.
    override fun configure(audioSinkConfig: AudioSink.AudioSinkConfig) {
        val format = audioSinkConfig.format
        if (gapless() || (format.encoderDelay == 0 && format.encoderPadding == 0)) {
            super.configure(audioSinkConfig)
            return
        }
        super.configure(
            AudioSink.AudioSinkConfig.Builder(format.buildUpon().setEncoderDelay(0).setEncoderPadding(0).build())
                .setPreferredBufferSizeOverride(audioSinkConfig.preferredBufferSizeOverride)
                .setOutputChannelMapping(audioSinkConfig.outputChannelMapping)
                .setTimeline(audioSinkConfig.timeline)
                .setMediaPeriodId(audioSinkConfig.mediaPeriodId)
                .build(),
        )
    }
}
