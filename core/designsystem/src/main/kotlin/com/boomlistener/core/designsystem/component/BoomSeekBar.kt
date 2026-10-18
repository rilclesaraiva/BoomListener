// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.boomlistener.core.designsystem.theme.boomSpatialSpec
import kotlin.math.PI
import kotlin.math.sin

// Medidas da barra do player (só aqui, no design system).
private val TouchHeight = 44.dp
private val WaveStroke = 5.dp
private val TrackStroke = 4.dp
private val WaveAmplitude = 3.5.dp
private val WaveLength = 30.dp
private val ThumbRadius = 10.dp
private val ThumbRadiusDragging = 12.dp
private val ThumbGap = 6.dp
private val StopDotRadius = 2.5.dp

/** Uma volta da onda a cada ~2 s enquanto toca. */
private const val WAVE_RADIANS_PER_SECOND = 3.1415927f

/**
 * Barra de progresso da música: a parte já tocada é uma onda grossa que
 * corre enquanto toca (e fica reta ao pausar), com uma bolinha redonda no
 * ponto atual (a onda chega grudada nela), uma folga antes do resto da trilha, reto e com um
 * pontinho no fim. Tocar ou arrastar muda o ponto da música.
 *
 * @param progress de 0 a 1.
 * @param onSeek chamado ao soltar o dedo, com a nova posição de 0 a 1.
 */
@Composable
fun BoomSeekBar(
    progress: Float,
    isPlaying: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Enquanto o dedo está na barra, ela segue o dedo e não o player.
    var dragging by remember { mutableStateOf<Float?>(null) }
    val shown = (dragging ?: progress).coerceIn(0f, 1f)
    val seek by rememberUpdatedState(onSeek)

    // A onda achata ao pausar e ao arrastar, e volta ao tocar.
    val amplitudeScale by animateFloatAsState(
        targetValue = if (isPlaying && dragging == null) 1f else 0f,
        animationSpec = boomSpatialSpec(),
        label = "waveAmplitude",
    )
    val thumbScale by animateFloatAsState(
        targetValue = if (dragging != null) 1f else 0f,
        animationSpec = boomSpatialSpec(),
        label = "thumbGrow",
    )
    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                phase = (phase + (now - last) / 1_000_000_000f * WAVE_RADIANS_PER_SECOND) % (2 * PI.toFloat())
                last = now
            }
        }
    }

    val active = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(TouchHeight)
            .pointerInput(Unit) {
                detectTapGestures { offset -> seek((offset.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset -> dragging = (offset.x / size.width).coerceIn(0f, 1f) },
                    onDragEnd = {
                        dragging?.let { seek(it) }
                        dragging = null
                    },
                    onDragCancel = { dragging = null },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        dragging = (change.position.x / size.width).coerceIn(0f, 1f)
                    },
                )
            },
    ) {
        val thumbRadius = ThumbRadius.toPx() + (ThumbRadiusDragging.toPx() - ThumbRadius.toPx()) * thumbScale
        val startX = thumbRadius
        val endX = size.width - thumbRadius
        val cy = size.height / 2
        val thumbX = startX + (endX - startX) * shown
        val gap = ThumbGap.toPx()

        // Resto da trilha, reto, com o pontinho de fim.
        val restStart = thumbX + thumbRadius + gap
        if (restStart < endX) {
            drawLine(inactive, Offset(restStart, cy), Offset(endX, cy), TrackStroke.toPx(), StrokeCap.Round)
        }
        drawCircle(active, StopDotRadius.toPx(), Offset(endX, cy))

        // Parte tocada: a onda vai até o centro da bolinha (grudada nela, sem folga).
        val waveEnd = thumbX
        if (waveEnd > startX) {
            val amplitude = WaveAmplitude.toPx() * amplitudeScale
            val k = (2 * PI / WaveLength.toPx()).toFloat()
            val path = Path()
            var x = startX
            path.moveTo(x, cy + amplitude * sin(k * x - phase))
            val step = 2f
            while (x < waveEnd) {
                x = (x + step).coerceAtMost(waveEnd)
                path.lineTo(x, cy + amplitude * sin(k * x - phase))
            }
            drawPath(path, active, style = Stroke(width = WaveStroke.toPx(), cap = StrokeCap.Round))
        }

        // Bolinha no ponto atual.
        drawCircle(active, thumbRadius, Offset(thumbX, cy))
    }
}

/** Linha fina de progresso, para o mini player. */
@Composable
fun BoomThinProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier.fillMaxWidth(),
        color = color,
        // Trilho bem suave no mesmo tom, para a barra se ler como "progresso".
        trackColor = color.copy(alpha = 0.22f),
        drawStopIndicator = {},
    )
}
