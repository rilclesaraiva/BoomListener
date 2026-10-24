// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.roundToLong

/**
 * Escolha de um trecho de tamanho fixo dentro de uma música: a barra é a
 * música inteira e a janela destacada é o trecho, com o começo e o fim
 * marcados. Arrastar a janela (ou tocar fora dela, que a centraliza ali)
 * muda o começo. [playheadMs] (dentro do trecho) mostra o que está tocando.
 */
@Composable
fun BoomClipSelector(
    totalMs: Long,
    startMs: Long,
    lengthMs: Long,
    playheadMs: Long?,
    onStartChange: (Long) -> Unit,
    onChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val currentStart by rememberUpdatedState(startMs)
    val change by rememberUpdatedState(onStartChange)
    val finished by rememberUpdatedState(onChangeFinished)
    val maxStart = (totalMs - lengthMs).coerceAtLeast(0)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(ClipSelectorHeight)
            .pointerInput(totalMs, lengthMs, enabled) {
                if (!enabled || totalMs <= 0 || maxStart <= 0) return@pointerInput
                val width = size.width.toFloat()
                fun msAt(x: Float) = (x / width * totalMs).roundToLong()
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val windowStart = currentStart.toFloat() / totalMs * width
                    val windowEnd = (currentStart + lengthMs).toFloat() / totalMs * width
                    // Segurou a janela: arrasta por onde pegou. Fora dela: a janela vem para o dedo.
                    val grab = if (down.position.x in windowStart..windowEnd) {
                        down.position.x - windowStart
                    } else {
                        ((windowEnd - windowStart) / 2).also {
                            change((msAt(down.position.x - it)).coerceIn(0, maxStart))
                        }
                    }
                    down.consume()
                    horizontalDrag(down.id) { move ->
                        change(msAt(move.position.x - grab).coerceIn(0, maxStart))
                        move.consume()
                    }
                    finished()
                }
            },
    ) {
        val w = size.width
        val h = size.height
        val track = TrackHeight.toPx()
        drawRoundRect(
            color = scheme.surfaceContainerHighest,
            topLeft = Offset(0f, (h - track) / 2),
            size = Size(w, track),
            cornerRadius = CornerRadius(track / 2),
        )
        if (totalMs <= 0) return@Canvas

        val minWidth = MinWindowWidth.toPx()
        var x0 = startMs.toFloat() / totalMs * w
        var x1 = (startMs + lengthMs).coerceAtMost(totalMs).toFloat() / totalMs * w
        if (x1 - x0 < minWidth) {
            val middle = (x0 + x1) / 2
            x0 = (middle - minWidth / 2).coerceIn(0f, w - minWidth)
            x1 = x0 + minWidth
        }
        val radius = CornerRadius(WindowCorner.toPx())
        drawRoundRect(scheme.primaryContainer, Offset(x0, 0f), Size(x1 - x0, h), radius)

        // O que já tocou do trecho, e o ponto que está tocando.
        if (playheadMs != null && lengthMs > 0) {
            val px = x0 + (x1 - x0) * (playheadMs.toFloat() / lengthMs).coerceIn(0f, 1f)
            drawRoundRect(scheme.primary.copy(alpha = PlayedAlpha), Offset(x0, 0f), Size(px - x0, h), radius)
            val line = PlayheadWidth.toPx()
            drawRoundRect(
                scheme.onPrimaryContainer,
                Offset(px - line / 2, h * 0.18f),
                Size(line, h * 0.64f),
                CornerRadius(line / 2),
            )
        }
        drawRoundRect(scheme.primary, Offset(x0, 0f), Size(x1 - x0, h), radius, style = Stroke(BorderWidth.toPx()))

        // Começo e fim marcados por dois puxadores.
        val handle = HandleWidth.toPx()
        val handleHeight = h * 0.5f
        listOf(x0 + BorderWidth.toPx() * 2, x1 - BorderWidth.toPx() * 2 - handle).forEach { x ->
            drawRoundRect(scheme.primary, Offset(x, (h - handleHeight) / 2), Size(handle, handleHeight), CornerRadius(handle / 2))
        }
    }
}

private val ClipSelectorHeight = 48.dp
private val TrackHeight = 12.dp
private val MinWindowWidth = 28.dp
private val WindowCorner = 12.dp
private val BorderWidth = 2.dp
private val HandleWidth = 4.dp
private val PlayheadWidth = 3.dp
private const val PlayedAlpha = 0.3f
