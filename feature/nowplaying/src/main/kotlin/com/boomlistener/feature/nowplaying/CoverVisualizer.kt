// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import com.boomlistener.core.player.AudioVisualizer
import com.boomlistener.core.player.VISUALIZER_BANDS
import com.boomlistener.core.player.VISUALIZER_WAVE_POINTS
import com.boomlistener.core.player.VisualizerStyle
import com.boomlistener.core.player.smoothVisualizer

/**
 * Visualizador por cima da parte de baixo da capa: barras (graves à esquerda,
 * agudos à direita) ou a onda do som, na cor de destaque do player, sobre um
 * escurecido suave para aparecer em qualquer capa. Acompanha o som que está
 * saindo agora ([AudioVisualizer]); pausado, as barras descem devagar.
 */
@Composable
internal fun CoverVisualizer(style: VisualizerStyle, playing: Boolean, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val scrim = MaterialTheme.colorScheme.scrim
    val bands = remember { FloatArray(VISUALIZER_BANDS) }
    val wave = remember { FloatArray(VISUALIZER_WAVE_POINTS) }
    val shownBands = remember { FloatArray(VISUALIZER_BANDS) }
    val shownWave = remember { FloatArray(VISUALIZER_WAVE_POINTS) }
    var frame by remember { mutableLongStateOf(0L) }

    // Só analisa o som enquanto o visualizador está na tela (com o app aberto).
    LifecycleStartEffect(Unit) {
        AudioVisualizer.attach()
        onStopOrDispose { AudioVisualizer.detach() }
    }
    LaunchedEffect(playing) {
        var settled = false
        // Pausado, desce até zerar e para de redesenhar (sem gastar bateria).
        while (!settled) {
            withFrameNanos { now ->
                val fresh = playing && AudioVisualizer.read(bands, wave)
                var energy = 0f
                for (i in shownBands.indices) {
                    shownBands[i] = smoothVisualizer(shownBands[i], if (fresh) bands[i] else 0f)
                    energy += shownBands[i]
                }
                for (i in shownWave.indices) {
                    shownWave[i] = smoothVisualizer(shownWave[i], if (fresh) wave[i] else 0f, rise = 0.5f, fall = 0.3f)
                    energy += kotlin.math.abs(shownWave[i])
                }
                frame = now
                settled = !playing && energy < 0.01f
            }
        }
    }

    Canvas(modifier) {
        frame // redesenha a cada quadro
        val w = size.width
        val h = size.height
        // Escurecido de baixo para cima, só na faixa do visualizador.
        drawRect(
            brush = Brush.verticalGradient(
                0f to scrim.copy(alpha = 0f),
                1f to scrim.copy(alpha = 0.55f),
                startY = h * 0.45f,
                endY = h,
            ),
            topLeft = Offset(0f, h * 0.45f),
            size = Size(w, h * 0.55f),
        )
        when (style) {
            VisualizerStyle.Bars -> {
                val side = 16.dp.toPx()
                val gap = 6.dp.toPx()
                val count = shownBands.size
                val barWidth = ((w - 2 * side) - gap * (count - 1)) / count
                val bottom = h - 12.dp.toPx()
                val maxHeight = h * 0.38f
                for (i in 0 until count) {
                    val barHeight = (maxHeight * shownBands[i]).coerceAtLeast(barWidth)
                    val left = side + i * (barWidth + gap)
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            0f to accent,
                            1f to accent.copy(alpha = 0.15f),
                            startY = bottom - barHeight,
                            endY = bottom,
                        ),
                        topLeft = Offset(left, bottom - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2),
                    )
                }
            }
            VisualizerStyle.Wave -> {
                val centerY = h * 0.8f
                val amplitude = h * 0.14f
                val step = w / (shownWave.size - 1)
                val path = Path()
                for (i in shownWave.indices) {
                    val x = i * step
                    // As pontas ficam paradas no meio: a onda nasce e morre nas bordas.
                    val edge = minOf(i, shownWave.size - 1 - i).coerceAtMost(6) / 6f
                    val y = centerY - shownWave[i] * amplitude * edge
                    if (i == 0) {
                        path.moveTo(x, y)
                    } else {
                        val px = (i - 1) * step
                        val previousEdge = minOf(i - 1, shownWave.size - i).coerceAtMost(6) / 6f
                        val py = centerY - shownWave[i - 1] * amplitude * previousEdge
                        path.quadraticTo(px, py, (px + x) / 2, (py + y) / 2)
                    }
                }
                path.lineTo(w, centerY)
                // Um brilho largo e fraco por baixo e a linha por cima.
                drawPath(path, accent.copy(alpha = 0.25f), style = Stroke(10.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(path, accent, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}
