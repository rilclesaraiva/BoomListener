// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.layout
import androidx.compose.ui.Alignment
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlin.math.abs
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
/** Distância em que a bolinha arrastada gruda no ponto que está tocando (ímã). */
private val SnapDistance = 14.dp

/** Uma volta da onda a cada ~2 s enquanto toca. */
private const val WAVE_RADIANS_PER_SECOND = 3.1415927f

/**
 * Barra de progresso da música: a parte já tocada é uma onda grossa que
 * corre enquanto toca (e fica reta ao pausar), com uma bolinha redonda no
 * ponto atual (a onda chega grudada nela), uma folga antes do resto da trilha, reto e com um
 * pontinho no fim. Tocar ou arrastar muda o ponto da música.
 *
 * Enquanto arrasta, a onda continua mostrando o que está tocando de verdade
 * e só a bolinha segue o dedo. Perto do ponto que está tocando, a bolinha
 * gruda nele (ímã): soltar ali não muda nada, o jeito fácil de desistir.
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
    /** Texto do balão sobre a bolinha enquanto arrasta (ex.: "2:54"); null = sem balão. */
    dragLabel: ((Float) -> String)? = null,
    /** Avisa onde o dedo está enquanto arrasta (null ao soltar), para a tela mostrar o tempo. */
    onDragPosition: (Float?) -> Unit = {},
) {
    // Onde o dedo está (null = sem arrastar). A bolinha mostra isso, ou o
    // ponto que está tocando quando o dedo está perto dele (ímã).
    var finger by remember { mutableStateOf<Float?>(null) }
    var widthPx by remember { mutableIntStateOf(0) }
    val snapPx = with(LocalDensity.current) { SnapDistance.toPx() }
    val current by rememberUpdatedState(progress.coerceIn(0f, 1f))
    val snappedState = remember(snapPx) {
        derivedStateOf { finger?.let { widthPx > 0 && abs(it - current) * widthPx <= snapPx } ?: false }
    }
    val snapped by snappedState
    val draggingState = remember { derivedStateOf { finger?.let { if (snappedState.value) current else it } } }
    val dragging: Float? by draggingState
    val dragReport by rememberUpdatedState(onDragPosition)
    LaunchedEffect(Unit) { snapshotFlow { dragging }.collect { dragReport(it) } }
    // Um toquinho de vibração ao grudar no ponto atual.
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(snapped) { if (snapped) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    val played = current
    val shown = (dragging ?: played).coerceIn(0f, 1f)
    val seek by rememberUpdatedState(onSeek)

    // A onda achata ao pausar e volta ao tocar.
    val amplitudeScale by animateFloatAsState(
        // Arrastando, a onda continua correndo: a música não parou.
        targetValue = if (isPlaying) 1f else 0f,
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

    Box(modifier.fillMaxWidth()) {
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(TouchHeight)
            .onSizeChanged { widthPx = it.width }
            .pointerInput(Unit) {
                detectTapGestures { offset -> seek((offset.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset -> finger = (offset.x / size.width).coerceIn(0f, 1f) },
                    onDragEnd = {
                        val at = finger
                        // Grudado no ponto atual: fica onde está, sem pular.
                        val stuck = at != null && abs(at - current) * size.width <= snapPx
                        if (at != null && !stuck) seek(at)
                        finger = null
                    },
                    onDragCancel = { finger = null },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        finger = (change.position.x / size.width).coerceIn(0f, 1f)
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

        val playedX = startX + (endX - startX) * played
        // Arrastando, a bolinha abre um buraco na trilha; fora disso a onda
        // chega grudada nela.
        val holeStart = if (dragging != null) thumbX - thumbRadius - gap else thumbX
        val holeEnd = thumbX + thumbRadius + gap
        val waveEnd = if (dragging != null) playedX else thumbX

        // Resto da trilha, reto (do fim do tocado até o fim), sem passar pela bolinha.
        val restFrom = maxOf(waveEnd, startX)
        fun rest(from: Float, to: Float) {
            if (to - from > 0.5f) drawLine(inactive, Offset(from, cy), Offset(to, cy), TrackStroke.toPx(), StrokeCap.Round)
        }
        if (restFrom < holeStart) rest(restFrom, minOf(holeStart, endX))
        rest(maxOf(restFrom, holeEnd), endX)
        // O pontinho do fim some aos poucos quando a bolinha chega perto
        // (senão parece que a música acabou antes da hora).
        val dotFade = ((endX - holeEnd) / (thumbRadius * 2)).coerceIn(0f, 1f)
        if (dotFade > 0f) drawCircle(active.copy(alpha = active.alpha * dotFade), StopDotRadius.toPx(), Offset(endX, cy))

        // Parte tocada: a onda. Arrastando, ela continua seguindo a música e
        // só desvia da bolinha.
        val amplitude = WaveAmplitude.toPx() * amplitudeScale
        val k = (2 * PI / WaveLength.toPx()).toFloat()
        fun wave(from: Float, to: Float) {
            if (to - from <= 0.5f) return
            val path = Path()
            var x = from
            path.moveTo(x, cy + amplitude * sin(k * x - phase))
            while (x < to) {
                x = (x + 2f).coerceAtMost(to)
                path.lineTo(x, cy + amplitude * sin(k * x - phase))
            }
            drawPath(path, active, style = Stroke(width = WaveStroke.toPx(), cap = StrokeCap.Round))
        }
        if (dragging == null) {
            wave(startX, waveEnd)
        } else {
            wave(startX, minOf(waveEnd, holeStart))
            wave(maxOf(startX, holeEnd), waveEnd)
        }

        // Bolinha no ponto atual.
        drawCircle(active, thumbRadius, Offset(thumbX, cy))
    }
    // Balão com o tempo sobre a bolinha, enquanto arrasta.
    val position = dragging
    if (dragLabel != null && position != null) {
        val gap = with(LocalDensity.current) { 6.dp.roundToPx() }
        Surface(
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.TopStart)
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                    layout(0, 0) {
                        val x = (position * widthPx - placeable.width / 2f).roundToInt()
                            .coerceIn(0, (widthPx - placeable.width).coerceAtLeast(0))
                        placeable.place(x, -placeable.height - gap)
                    }
                },
        ) {
            Text(
                dragLabel(position),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
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
