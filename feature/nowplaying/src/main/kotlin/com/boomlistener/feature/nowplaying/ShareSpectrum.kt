// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * As barrinhas do vídeo, como um equalizador: da esquerda para a direita,
 * dos graves aos agudos (faixas de frequência em escala logarítmica, como no
 * ouvido). A cada [LEVEL_WINDOW_MS] do trecho mede-se a energia de cada faixa
 * (FFT com janela de Hann); cada quadro do vídeo interpola entre as medidas
 * vizinhas para as barras subirem e descerem sem saltos.
 */

private const val FFT_SIZE = 4096
private const val MIN_HZ = 40.0
private const val MAX_HZ = 16_000.0

/** Faixas de frequência por medida: [frame][barra], de 0 a 1. */
internal fun spectrumFrames(mono: FloatArray, sampleRate: Int, bars: Int = WAVE_BARS): Array<FloatArray> {
    val hop = (sampleRate * LEVEL_WINDOW_MS / 1000).toInt().coerceAtLeast(1)
    val count = (mono.size / hop).coerceAtLeast(1)
    val window = FloatArray(FFT_SIZE) { (0.5 - 0.5 * cos(2 * PI * it / (FFT_SIZE - 1))).toFloat() }
    val edges = DoubleArray(bars + 1) { MIN_HZ * (MAX_HZ / MIN_HZ).pow(it.toDouble() / bars) }
    val binHz = sampleRate.toDouble() / FFT_SIZE
    val re = DoubleArray(FFT_SIZE)
    val im = DoubleArray(FFT_SIZE)
    val raw = Array(count) { frame ->
        val start = frame * hop
        for (i in 0 until FFT_SIZE) {
            val index = start + i
            re[i] = if (index < mono.size) (mono[index] * window[i]).toDouble() else 0.0
            im[i] = 0.0
        }
        fft(re, im)
        FloatArray(bars) { bar ->
            val from = (edges[bar] / binHz).toInt().coerceIn(1, FFT_SIZE / 2 - 1)
            val to = (edges[bar + 1] / binHz).toInt().coerceIn(from + 1, FFT_SIZE / 2)
            var energy = 0.0
            for (bin in from until to) energy = max(energy, sqrt(re[bin] * re[bin] + im[bin] * im[bin]))
            // Em decibéis (o ouvido percebe assim), a partir de zero.
            (20 * log10(1 + energy)).toFloat()
        }
    }
    // Cada faixa vai até o topo no momento mais forte dela no trecho (graves
    // sempre têm mais energia; sem isso os agudos nunca subiriam), misturado
    // com a escala geral para o volume do trecho ainda contar.
    val overall = raw.maxOf { it.max() }.coerceAtLeast(1e-6f)
    val peaks = FloatArray(bars) { bar -> raw.maxOf { it[bar] }.coerceAtLeast(1e-6f) }
    return Array(count) { frame ->
        FloatArray(bars) { bar ->
            val v = raw[frame][bar]
            (0.6f * v / peaks[bar] + 0.4f * v / overall).coerceIn(0f, 1f)
        }
    }
}

/** Alturas das barras no instante [timeMs] do trecho (graves à esquerda). */
internal fun spectrumBars(frames: Array<FloatArray>, timeMs: Long, bars: Int = WAVE_BARS): FloatArray {
    if (frames.isEmpty()) return FloatArray(bars) { MIN_BAR }
    val position = (timeMs.toFloat() / LEVEL_WINDOW_MS).coerceIn(0f, (frames.size - 1).toFloat())
    val a = frames[position.toInt()]
    val b = frames[(position.toInt() + 1).coerceAtMost(frames.size - 1)]
    val t = position - position.toInt()
    return FloatArray(bars) { bar ->
        val v = if (bar < a.size) a[bar] + (b[bar] - a[bar]) * t else 0f
        MIN_BAR + (1f - MIN_BAR) * v.coerceIn(0f, 1f)
    }
}

/** FFT radix-2 no lugar (tamanho potência de 2). */
internal fun fft(re: DoubleArray, im: DoubleArray) {
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
    var len = 2
    while (len <= n) {
        val angle = -2 * PI / len
        val wRe = cos(angle)
        val wIm = sin(angle)
        var i = 0
        while (i < n) {
            var curRe = 1.0
            var curIm = 0.0
            for (k in 0 until len / 2) {
                val uRe = re[i + k]
                val uIm = im[i + k]
                val vRe = re[i + k + len / 2] * curRe - im[i + k + len / 2] * curIm
                val vIm = re[i + k + len / 2] * curIm + im[i + k + len / 2] * curRe
                re[i + k] = uRe + vRe
                im[i + k] = uIm + vIm
                re[i + k + len / 2] = uRe - vRe
                im[i + k + len / 2] = uIm - vIm
                val next = curRe * wRe - curIm * wIm
                curIm = curRe * wIm + curIm * wRe
                curRe = next
            }
            i += len
        }
        len = len shl 1
    }
}
