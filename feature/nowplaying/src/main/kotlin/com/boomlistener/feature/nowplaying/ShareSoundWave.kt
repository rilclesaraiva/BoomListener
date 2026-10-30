// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.core.graphics.ColorUtils
import kotlin.math.abs

/*
 * A onda do banner: o mesmo desenho do som que o visualizador "Onda" do
 * player mostra sobre a capa (as próprias amostras do áudio, não as faixas de
 * frequência). A cada [LEVEL_WINDOW_MS] do trecho guarda-se o desenho de uma
 * janela de [SOUND_WAVE_WINDOW] amostras em [SOUND_WAVE_POINTS] pontos; cada
 * quadro do vídeo mistura as medidas vizinhas.
 */

/** Amostras de cada medida (as mesmas do visualizador do player). */
internal const val SOUND_WAVE_WINDOW = 1024

/** Pontos da onda (os mesmos do visualizador do player). */
internal const val SOUND_WAVE_POINTS = 64

/**
 * O desenho do som a cada [LEVEL_WINDOW_MS] de [mono] (−1 a 1), ampliado para
 * o pico do trecho chegar perto de 1 (trecho baixinho ainda aparece).
 */
internal fun soundWaveFrames(mono: FloatArray, sampleRate: Int): Array<FloatArray> {
    if (mono.isEmpty() || sampleRate <= 0) return emptyArray()
    val hop = (sampleRate * LEVEL_WINDOW_MS / 1000).toInt().coerceAtLeast(1)
    val count = (mono.size / hop).coerceAtLeast(1)
    var peak = 0f
    for (v in mono) peak = maxOf(peak, abs(v))
    val gain = 1f / peak.coerceAtLeast(MIN_PEAK)
    return Array(count) { frame ->
        val start = frame * hop
        FloatArray(SOUND_WAVE_POINTS) { i ->
            val index = start + (i.toLong() * (SOUND_WAVE_WINDOW - 1) / (SOUND_WAVE_POINTS - 1)).toInt()
            (if (index < mono.size) mono[index] * gain else 0f).coerceIn(-1f, 1f)
        }
    }
}

/** A onda no instante [timeMs] do trecho: mistura das duas medidas vizinhas. */
internal fun soundWaveAt(frames: Array<FloatArray>, timeMs: Long): FloatArray {
    if (frames.isEmpty()) return FloatArray(SOUND_WAVE_POINTS)
    val position = (timeMs.toFloat() / LEVEL_WINDOW_MS).coerceIn(0f, (frames.size - 1).toFloat())
    val a = frames[position.toInt()]
    val b = frames[(position.toInt() + 1).coerceAtMost(frames.size - 1)]
    val t = position - position.toInt()
    return FloatArray(SOUND_WAVE_POINTS) { i -> a[i] + (b[i] - a[i]) * t }
}

/** A medida mais forte do trecho (a imagem parada mostra a onda nela). */
internal fun loudestFrame(levels: FloatArray): Int {
    var best = 0
    for (i in levels.indices) if (levels[i] > levels[best]) best = i
    return best
}

/** Sem leitura do som: uma onda calma e igual sempre (para a imagem não ficar vazia). */
internal fun calmSoundWave(): FloatArray =
    FloatArray(SOUND_WAVE_POINTS) { i -> (kotlin.math.sin(i * 0.45) * 0.35 * kotlin.math.sin(i * 0.11)).toFloat() }

/**
 * Desenha a onda centrada em ([centerX], [centerY]): uma linha lisa com um
 * brilho largo e fraco por baixo, as pontas presas no meio (a onda nasce e
 * morre nas bordas), como no visualizador do player.
 */
internal fun drawSoundWave(canvas: Canvas, centerX: Float, centerY: Float, points: FloatArray, color: Int) {
    if (points.size < 2) return
    val left = centerX - SOUND_WAVE_WIDTH / 2
    val step = SOUND_WAVE_WIDTH / (points.size - 1)
    fun yAt(i: Int): Float {
        val edge = minOf(i, points.lastIndex - i).coerceAtMost(EDGE_POINTS) / EDGE_POINTS.toFloat()
        return centerY - points[i] * SOUND_WAVE_AMPLITUDE * edge
    }
    val path = Path()
    path.moveTo(left, yAt(0))
    for (i in 1 until points.size) {
        val px = left + (i - 1) * step
        val x = left + i * step
        path.quadTo(px, yAt(i - 1), (px + x) / 2, (yAt(i - 1) + yAt(i)) / 2)
    }
    path.lineTo(left + SOUND_WAVE_WIDTH, centerY)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    paint.color = ColorUtils.setAlphaComponent(color, GLOW_ALPHA)
    paint.strokeWidth = GLOW_WIDTH
    canvas.drawPath(path, paint)
    paint.color = color
    paint.strokeWidth = LINE_WIDTH
    canvas.drawPath(path, paint)
}

private const val MIN_PEAK = 0.05f
private const val EDGE_POINTS = 6
private const val SOUND_WAVE_WIDTH = 720f
private const val SOUND_WAVE_AMPLITUDE = 95f
private const val GLOW_ALPHA = 0x40
private const val GLOW_WIDTH = 26f
private const val LINE_WIDTH = 8f
