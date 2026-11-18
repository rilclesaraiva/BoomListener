// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.boomlistener.core.designsystem.theme.WrappedCardColors

/**
 * Fundo do Wrapped: formas abstratas (discos, anéis, arcos) desenhadas atrás do cartão, no tom
 * dele, para dar profundidade e personalidade em vez de uma cor lisa. [variant] escolhe uma de
 * quatro composições (a mesma para o mesmo cartão), e as formas são suaves o bastante para o
 * texto continuar legível. Só desenha: não tem toque nem texto.
 */
@Composable
fun WrappedBackdrop(colors: WrappedCardColors, variant: Int, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        // Um degradê escuro por baixo dá peso à parte de baixo do cartão.
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.22f))))
        when (((variant % COMPOSITIONS) + COMPOSITIONS) % COMPOSITIONS) {
            0 -> discs(colors)
            1 -> rings(colors)
            2 -> arcs(colors)
            else -> blobs(colors)
        }
    }
}

private const val COMPOSITIONS = 4

/** Discos grandes que saem das bordas, sobrepostos. */
private fun DrawScope.discs(colors: WrappedCardColors) {
    drawCircle(colors.accent.copy(alpha = 0.16f), radius = size.width * 0.62f, center = Offset(size.width * 1.0f, size.height * 0.08f))
    drawCircle(colors.onContainer.copy(alpha = 0.07f), radius = size.width * 0.48f, center = Offset(size.width * 0.02f, size.height * 0.62f))
    drawCircle(Color.Black.copy(alpha = 0.16f), radius = size.width * 0.38f, center = Offset(size.width * 0.86f, size.height * 0.94f))
    drawCircle(colors.accent.copy(alpha = 0.12f), radius = size.width * 0.12f, center = Offset(size.width * 0.3f, size.height * 0.3f))
}

/** Anéis concêntricos que se abrem a partir de um canto. */
private fun DrawScope.rings(colors: WrappedCardColors) {
    val center = Offset(size.width * 0.05f, size.height * 0.9f)
    repeat(6) { index ->
        val radius = size.width * (0.28f + index * 0.17f)
        drawCircle(
            color = colors.accent.copy(alpha = 0.2f - index * 0.025f),
            radius = radius,
            center = center,
            style = Stroke(width = size.width * 0.02f),
        )
    }
    drawCircle(colors.onContainer.copy(alpha = 0.08f), radius = size.width * 0.34f, center = Offset(size.width * 0.98f, size.height * 0.12f))
    drawCircle(Color.Black.copy(alpha = 0.14f), radius = size.width * 0.2f, center = Offset(size.width * 0.8f, size.height * 0.3f))
}

/** Arcos largos, como ondas que cruzam o cartão na diagonal. */
private fun DrawScope.arcs(colors: WrappedCardColors) {
    val side = size.width * 1.5f
    drawArc(
        color = colors.accent.copy(alpha = 0.15f),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(size.width * 0.5f - side / 2f, size.height * 0.62f),
        size = Size(side, side),
    )
    drawArc(
        color = Color.Black.copy(alpha = 0.14f),
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(-size.width * 0.3f, -side * 0.45f),
        size = Size(side, side),
    )
    drawArc(
        color = colors.onContainer.copy(alpha = 0.1f),
        startAngle = 270f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(size.width * 0.15f, size.height * 0.2f),
        size = Size(size.width * 1.1f, size.width * 1.1f),
        style = Stroke(width = size.width * 0.05f),
    )
}

/** Manchas ovais soltas, em dois tamanhos. */
private fun DrawScope.blobs(colors: WrappedCardColors) {
    drawOval(colors.accent.copy(alpha = 0.15f), topLeft = Offset(size.width * 0.35f, -size.height * 0.06f), size = Size(size.width * 0.9f, size.height * 0.34f))
    drawOval(colors.onContainer.copy(alpha = 0.07f), topLeft = Offset(-size.width * 0.3f, size.height * 0.4f), size = Size(size.width * 0.8f, size.height * 0.3f))
    drawOval(Color.Black.copy(alpha = 0.15f), topLeft = Offset(size.width * 0.2f, size.height * 0.82f), size = Size(size.width * 1.0f, size.height * 0.3f))
    drawCircle(colors.accent.copy(alpha = 0.12f), radius = size.width * 0.1f, center = Offset(size.width * 0.84f, size.height * 0.56f))
}
