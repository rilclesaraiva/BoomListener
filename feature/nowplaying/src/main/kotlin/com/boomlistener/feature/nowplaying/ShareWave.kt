// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying


/*
 * As barrinhas do banner, tiradas do som do próprio trecho.
 *
 * [levels] é a força do som (RMS) a cada [LEVEL_WINDOW_MS] do trecho, lida do
 * arquivo (ClipLevels.kt). Na imagem, cada barra é a média de um pedaço do
 * trecho (o desenho dos 30 s, da esquerda para a direita). No vídeo, as
 * barras são um equalizador (graves à esquerda, agudos à direita): ShareSpectrum.kt.
 * Alturas vão de [MIN_BAR] (silêncio) a 1 (o mais alto do trecho).
 */

internal const val LEVEL_WINDOW_MS = 50L
internal const val WAVE_BARS = 27
internal const val MIN_BAR = 0.12f

/** Desenho fixo dos 30 s: [bars] barras, cada uma a média de um pedaço do trecho. */
internal fun waveformBars(levels: FloatArray, bars: Int = WAVE_BARS): FloatArray {
    if (levels.isEmpty()) return FloatArray(bars) { MIN_BAR }
    val means = FloatArray(bars) { bar ->
        val from = (bar.toLong() * levels.size / bars).toInt()
        val to = (((bar + 1).toLong() * levels.size / bars).toInt()).coerceAtLeast(from + 1).coerceAtMost(levels.size)
        if (from >= levels.size) 0f else (from until to).map { levels[it] }.average().toFloat()
    }
    return scaled(means)
}

private fun scaled(values: FloatArray): FloatArray {
    val peak = values.max()
    return FloatArray(values.size) { height(values[it], peak) }
}

private fun height(level: Float, peak: Float): Float =
    if (peak <= 0f) MIN_BAR else MIN_BAR + (1f - MIN_BAR) * (level / peak).coerceIn(0f, 1f)
