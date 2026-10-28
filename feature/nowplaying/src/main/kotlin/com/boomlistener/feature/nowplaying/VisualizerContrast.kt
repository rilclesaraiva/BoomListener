// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import kotlin.math.pow

/*
 * Quanto escurecer atrás do visualizador (puro e testado): a cor dele é a de
 * destaque do player, que sai da própria capa. Quando a parte de baixo da capa
 * tem cores parecidas com ela (pouco contraste), o escurecido fica mais forte,
 * senão as barras somem no fundo.
 */

/** Escurecido normal e o máximo (capa da mesma cor do visualizador). */
internal const val VISUALIZER_SCRIM_MIN = 0.55f
internal const val VISUALIZER_SCRIM_MAX = 0.88f

/** Abaixo deste contraste (escala WCAG, 1 a 21) a cor da capa "se confunde" com a do visualizador. */
private const val LOW_CONTRAST = 2.2

/**
 * [pixels]: amostras (ARGB) da parte de baixo da capa; [accent]: cor do
 * visualizador. Metade ou mais das amostras parecidas já dá o máximo.
 */
internal fun visualizerScrimAlpha(pixels: IntArray, accent: Int): Float {
    if (pixels.isEmpty()) return VISUALIZER_SCRIM_MIN
    val accentLuminance = luminance(accent)
    val close = pixels.count { contrast(luminance(it), accentLuminance) < LOW_CONTRAST }
    val share = (close.toFloat() / pixels.size * 2f).coerceIn(0f, 1f)
    return VISUALIZER_SCRIM_MIN + (VISUALIZER_SCRIM_MAX - VISUALIZER_SCRIM_MIN) * share
}

private fun contrast(a: Double, b: Double): Double = (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)

/** Luminância relativa (WCAG) de uma cor ARGB. */
private fun luminance(color: Int): Double {
    fun channel(value: Int): Double {
        val c = value / 255.0
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel((color shr 16) and 0xFF) + 0.7152 * channel((color shr 8) and 0xFF) + 0.0722 * channel(color and 0xFF)
}
