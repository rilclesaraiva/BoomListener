// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.player

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * Contas do visualizador sobre a capa (puras e testadas): de um pedaço de
 * som (mono, -1 a 1) saem as barras (graves à esquerda, agudos à direita) e
 * a onda (o desenho do próprio som).
 */

/** Tipo do visualizador (Ajustes → Aparência). Oscilloscope: XY, esquerdo × direito. */
enum class VisualizerStyle { Bars, Wave, Oscilloscope }

/** Amostras analisadas de cada vez (≈ 23 ms a 44,1 kHz). */
internal const val VISUALIZER_WINDOW = 1024

/** Quantas barras. */
const val VISUALIZER_BANDS = 24

/** Quantos pontos na onda. */
const val VISUALIZER_WAVE_POINTS = 64

private const val MIN_HZ = 40.0
private const val MAX_HZ = 16_000.0
private const val FLOOR_DB = -60.0

/**
 * Força de cada faixa de frequência (0 a 1), em escala logarítmica de 40 Hz
 * a 16 kHz, como o ouvido percebe. Uma senoide no volume máximo dá 1.
 */
internal fun visualizerBands(window: FloatArray, sampleRate: Int, bands: Int = VISUALIZER_BANDS): FloatArray {
    val n = window.size
    val re = DoubleArray(n) { i -> window[i] * (0.5 - 0.5 * cos(2 * PI * i / (n - 1))) }
    val im = DoubleArray(n)
    fftInPlace(re, im)
    val binHz = sampleRate.toDouble() / n
    val top = minOf(MAX_HZ, sampleRate / 2.0)
    // Com a janela de Hann, uma senoide de amplitude 1 dá magnitude ≈ n/4 no pico.
    val reference = n / 4.0
    return FloatArray(bands) { band ->
        val low = MIN_HZ * (top / MIN_HZ).pow(band.toDouble() / bands)
        val high = MIN_HZ * (top / MIN_HZ).pow((band + 1).toDouble() / bands)
        fun magnitude(bin: Int) = sqrt(re[bin] * re[bin] + im[bin] * im[bin])
        // Faixas graves são mais estreitas que um "bin" da FFT: o valor no
        // meio da faixa sai da interpolação entre os dois bins vizinhos, senão
        // as primeiras barras ficariam iguais.
        val center = sqrt(low * high) / binHz
        val below = center.toInt().coerceIn(1, n / 2 - 2)
        val fraction = (center - below).coerceIn(0.0, 1.0)
        var peak = magnitude(below) * (1 - fraction) + magnitude(below + 1) * fraction
        // Faixas largas (agudos): o maior bin dentro dela.
        val first = kotlin.math.ceil(low / binHz).toInt().coerceIn(1, n / 2 - 1)
        val last = (high / binHz).toInt().coerceIn(1, n / 2 - 1)
        for (bin in first..last) peak = maxOf(peak, magnitude(bin))
        val db = 20 * log10((peak / reference).coerceAtLeast(1e-9))
        ((db - FLOOR_DB) / -FLOOR_DB).coerceIn(0.0, 1.0).toFloat()
    }
}

/** O desenho do som: [points] amostras espalhadas pelo pedaço, de -1 a 1. */
internal fun waveformPoints(window: FloatArray, points: Int = VISUALIZER_WAVE_POINTS): FloatArray {
    if (window.isEmpty()) return FloatArray(points)
    return FloatArray(points) { i ->
        window[(i.toLong() * (window.size - 1) / (points - 1).coerceAtLeast(1)).toInt()].coerceIn(-1f, 1f)
    }
}

/** FFT de Cooley-Tukey no lugar (tamanho potência de 2). */
internal fun fftInPlace(re: DoubleArray, im: DoubleArray) {
    val n = re.size
    var j = 0
    for (i in 1 until n) {
        var bit = n shr 1
        while (j and bit != 0) {
            j = j xor bit
            bit = bit shr 1
        }
        j = j xor bit
        if (i < j) {
            re[i] = re[j].also { re[j] = re[i] }
            im[i] = im[j].also { im[j] = im[i] }
        }
    }
    var length = 2
    while (length <= n) {
        val angle = -2 * PI / length
        val wRe = cos(angle)
        val wIm = sin(angle)
        var start = 0
        while (start < n) {
            var curRe = 1.0
            var curIm = 0.0
            for (k in 0 until length / 2) {
                val a = start + k
                val b = a + length / 2
                val tRe = re[b] * curRe - im[b] * curIm
                val tIm = re[b] * curIm + im[b] * curRe
                re[b] = re[a] - tRe
                im[b] = im[a] - tIm
                re[a] += tRe
                im[a] += tIm
                val next = curRe * wRe - curIm * wIm
                curIm = curRe * wIm + curIm * wRe
                curRe = next
            }
            start += length
        }
        length = length shl 1
    }
}

/**
 * Suaviza a barra entre um quadro e outro: sobe rápido (acompanha a batida)
 * e desce devagar (sem piscar).
 */
fun smoothVisualizer(previous: Float, target: Float, rise: Float = 0.6f, fall: Float = 0.15f): Float =
    previous + (target - previous) * if (target > previous) rise else fall
