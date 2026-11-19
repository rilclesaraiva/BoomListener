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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import com.boomlistener.core.designsystem.theme.WrappedCardColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Fundo do Wrapped: formas abstratas desenhadas atrás do cartão, no tom dele, para dar
 * profundidade e personalidade em vez de uma cor lisa. Cada [variant] (o número do cartão) é
 * uma composição própria, com tipo de forma diferente das outras nove (discos, anéis, arcos,
 * manchas, listras, pontos, losangos, raios, ondas e triângulos), então nenhum cartão repete o
 * desenho de outro. As formas são suaves o bastante para o texto continuar legível. Só desenha:
 * não tem toque nem texto.
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
            3 -> blobs(colors)
            4 -> stripes(colors)
            5 -> dots(colors)
            6 -> diamonds(colors)
            7 -> rays(colors)
            8 -> waves(colors)
            else -> triangles(colors)
        }
    }
}

/** Uma composição por cartão (o Wrapped tem no máximo dez). */
private const val COMPOSITIONS = 10

/** Discos grandes que saem das bordas, sobrepostos. */
private fun DrawScope.discs(colors: WrappedCardColors) {
    drawCircle(colors.accent.copy(alpha = 0.16f), radius = size.width * 0.62f, center = Offset(size.width * 1.0f, size.height * 0.08f))
    drawCircle(colors.onContainer.copy(alpha = 0.07f), radius = size.width * 0.48f, center = Offset(size.width * 0.02f, size.height * 0.62f))
    drawCircle(Color.Black.copy(alpha = 0.16f), radius = size.width * 0.38f, center = Offset(size.width * 0.86f, size.height * 0.94f))
    drawCircle(colors.accent.copy(alpha = 0.12f), radius = size.width * 0.12f, center = Offset(size.width * 0.3f, size.height * 0.3f))
}

/** Anéis concêntricos que se abrem a partir do canto de baixo à esquerda. */
private fun DrawScope.rings(colors: WrappedCardColors) {
    val center = Offset(size.width * 0.05f, size.height * 0.9f)
    repeat(6) { index ->
        drawCircle(
            color = colors.accent.copy(alpha = 0.2f - index * 0.025f),
            radius = size.width * (0.28f + index * 0.17f),
            center = center,
            style = Stroke(width = size.width * 0.02f),
        )
    }
    drawCircle(colors.onContainer.copy(alpha = 0.08f), radius = size.width * 0.34f, center = Offset(size.width * 0.98f, size.height * 0.12f))
}

/** Arcos largos: uma cúpula embaixo, outra em cima e um traço fino no meio. */
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
}

/** Faixas largas na diagonal, de espessuras diferentes. */
private fun DrawScope.stripes(colors: WrappedCardColors) {
    rotate(degrees = -24f, pivot = center) {
        val reach = size.height * 1.4f
        val bands = listOf(
            Triple(0.08f, 0.10f, colors.accent.copy(alpha = 0.16f)),
            Triple(0.26f, 0.04f, colors.onContainer.copy(alpha = 0.08f)),
            Triple(0.42f, 0.14f, Color.Black.copy(alpha = 0.12f)),
            Triple(0.70f, 0.06f, colors.accent.copy(alpha = 0.13f)),
            Triple(0.86f, 0.12f, colors.onContainer.copy(alpha = 0.06f)),
        )
        bands.forEach { (at, thick, color) ->
            drawRect(color, topLeft = Offset(-size.width * 0.3f, size.height * at - reach * 0.2f), size = Size(size.width * 1.6f, size.height * thick))
        }
    }
}

/** Grade de pontos que cresce de um canto ao outro, como uma retícula de jornal. */
private fun DrawScope.dots(colors: WrappedCardColors) {
    val columns = 9
    val step = size.width / columns
    val rows = (size.height / step).toInt() + 1
    for (row in 0..rows) {
        for (column in 0..columns) {
            val t = ((column.toFloat() / columns) + (row.toFloat() / rows)) / 2f
            val radius = step * (0.04f + 0.26f * t)
            drawCircle(colors.accent.copy(alpha = 0.06f + 0.12f * t), radius = radius, center = Offset(step * (column + 0.5f * (row % 2)), step * row))
        }
    }
}

/** Losangos encaixados (quadrados girados) crescendo de um ponto. */
private fun DrawScope.diamonds(colors: WrappedCardColors) {
    val origin = Offset(size.width * 0.9f, size.height * 0.2f)
    repeat(5) { index ->
        val half = size.width * (0.14f + index * 0.17f)
        val path = Path().apply {
            moveTo(origin.x, origin.y - half)
            lineTo(origin.x + half, origin.y)
            lineTo(origin.x, origin.y + half)
            lineTo(origin.x - half, origin.y)
            close()
        }
        drawPath(path, colors.accent.copy(alpha = 0.2f - index * 0.03f), style = Stroke(width = size.width * 0.018f))
    }
    val low = Offset(size.width * 0.1f, size.height * 0.88f)
    val half = size.width * 0.3f
    drawPath(
        Path().apply {
            moveTo(low.x, low.y - half); lineTo(low.x + half, low.y); lineTo(low.x, low.y + half); lineTo(low.x - half, low.y); close()
        },
        Color.Black.copy(alpha = 0.13f),
    )
}

/** Raios que abrem em leque a partir do canto de cima à direita. */
private fun DrawScope.rays(colors: WrappedCardColors) {
    val origin = Offset(size.width * 1.02f, -size.height * 0.02f)
    val reach = size.height * 1.3f
    val count = 9
    for (index in 0 until count) {
        val start = (PI / 2.0) + (PI / 2.0) * index / count
        val end = (PI / 2.0) + (PI / 2.0) * (index + 0.5) / count
        val path = Path().apply {
            moveTo(origin.x, origin.y)
            lineTo(origin.x + (reach * cos(start)).toFloat(), origin.y + (reach * sin(start)).toFloat())
            lineTo(origin.x + (reach * cos(end)).toFloat(), origin.y + (reach * sin(end)).toFloat())
            close()
        }
        drawPath(path, (if (index % 2 == 0) colors.accent else colors.onContainer).copy(alpha = 0.10f))
    }
}

/** Ondas horizontais empilhadas, como ondas sonoras na água. */
private fun DrawScope.waves(colors: WrappedCardColors) {
    val layers = 4
    for (layer in 0 until layers) {
        val baseline = size.height * (0.42f + layer * 0.15f)
        val amplitude = size.height * (0.025f + layer * 0.012f)
        val length = size.width * (0.7f - layer * 0.08f)
        val path = Path().apply {
            moveTo(0f, size.height)
            lineTo(0f, baseline)
            var x = 0f
            while (x <= size.width) {
                lineTo(x, baseline + amplitude * sin(2.0 * PI * (x / length) + layer).toFloat())
                x += size.width / 40f
            }
            lineTo(size.width, size.height)
            close()
        }
        val color = if (layer % 2 == 0) colors.accent.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.10f)
        drawPath(path, color)
    }
}

/** Triângulos soltos apontando para direções diferentes. */
private fun DrawScope.triangles(colors: WrappedCardColors) {
    fun triangle(a: Offset, b: Offset, c: Offset, color: Color) =
        drawPath(Path().apply { moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); close() }, color)
    val w = size.width
    val h = size.height
    triangle(Offset(w * 0.45f, 0f), Offset(w * 1.1f, 0f), Offset(w * 1.1f, h * 0.34f), colors.accent.copy(alpha = 0.16f))
    triangle(Offset(-w * 0.1f, h * 0.36f), Offset(w * 0.5f, h * 0.52f), Offset(-w * 0.1f, h * 0.7f), colors.onContainer.copy(alpha = 0.07f))
    triangle(Offset(w * 0.3f, h * 1.05f), Offset(w * 1.1f, h * 0.66f), Offset(w * 1.1f, h * 1.05f), Color.Black.copy(alpha = 0.15f))
    triangle(Offset(w * 0.12f, h * 0.12f), Offset(w * 0.3f, h * 0.2f), Offset(w * 0.14f, h * 0.3f), colors.accent.copy(alpha = 0.12f))
}
