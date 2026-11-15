// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.sqrt

/*
 * O ícone do BoomListener desenhado em código, para animar por partes na
 * tela Sobre ("Estrondo atrasado"): um caça (a bolinha) entra de fora pela
 * direita em silêncio, toma fôlego e a barreira do som quebra atrás dele: o
 * ícone treme, um clarão, ecos e os dois arcos saltam.
 *
 * As medidas seguem o ícone do app (app/src/main/res/drawable/ic_launcher_foreground.xml
 * e a cor de fundo em values/colors.xml): se o ícone mudar, mude aqui também.
 * Coordenadas em "unidades do ícone": a área visível é de 21 a 87 (66 unidades).
 *
 * Exceção à regra do movimento (nada de tempos à mão): é uma coreografia de
 * marca, com tempos próprios que as molas do Material não descrevem. Por isso
 * mora aqui, em core/designsystem.
 */

/** Duração da animação inteira, em milissegundos (o último eco termina em 1980). */
internal const val BoomLogoMs = 2000f

/** Espera ao entrar na tela, para a animação começar com a transição já assentada. */
private const val EntryDelayMs = 400L

private val IconBase = Color(0xFF9A4600)
private val IconPeach = Color(0xFFFFDBC8)
private val IconWhite = Color(0xFFFFFFFF)

private val JetEase = CubicBezierEasing(0.05f, 0.8f, 0.1f, 1f)
private val EaseOut = CubicBezierEasing(0f, 0f, 0.58f, 1f)
private val RingEaseA = CubicBezierEasing(0.1f, 0.7f, 0.2f, 1f)
private val RingEaseB = CubicBezierEasing(0.1f, 0.6f, 0.2f, 1f)
private val ConeEase = CubicBezierEasing(0.15f, 0.8f, 0.2f, 1f)
private val ArcEase = CubicBezierEasing(0.2f, 2f, 0.4f, 1f)

/** Uma onda que se abre a partir da bolinha. */
internal class RingFrame(val alpha: Float, val radius: Float)

/** Tudo o que se desenha num instante [t] (em ms) da animação. */
internal class BoomLogoFrame(
    /** Deslocamento do caça para a direita (60 → 0). */
    val jetX: Float,
    val flyAlpha: Float,
    val streakScaleX: Float,
    val dotScaleX: Float,
    val dotScaleY: Float,
    val shakeX: Float,
    val shakeY: Float,
    val flashAlpha: Float,
    val flashRadius: Float,
    val rings: List<RingFrame>,
    val coneAlpha: Float,
    val coneFraction: Float,
    val arc1Alpha: Float,
    val arc1Scale: Float,
    val arc2Alpha: Float,
    val arc2Scale: Float,
)

private fun phase(t: Float, delay: Float, duration: Float) = ((t - delay) / duration).coerceIn(0f, 1f)

private fun mix(a: Float, b: Float, p: Float) = a + (b - a) * p

/** Interpola por pontos (fração, valor); cada trecho com [easing]. */
private fun keys(p: Float, vararg points: Pair<Float, Float>, easing: Easing = EaseOut): Float {
    for (i in 1 until points.size) {
        if (p <= points[i].first) {
            val (p0, v0) = points[i - 1]
            val (p1, v1) = points[i]
            val local = if (p1 > p0) ((p - p0) / (p1 - p0)).coerceIn(0f, 1f) else 1f
            return mix(v0, v1, easing.transform(local))
        }
    }
    return points.last().second
}

internal fun boomLogoFrame(t: Float): BoomLogoFrame {
    val fly = phase(t, 0f, 400f)
    val charge = phase(t, 260f, 750f)
    val shake = phase(t, 720f, 260f)
    val flash = EaseOut.transform(phase(t, 720f, 500f))
    val cone = phase(t, 720f, 800f)
    val arc1 = phase(t, 740f, 550f)
    val arc2 = phase(t, 800f, 600f)
    fun ring(delay: Float, duration: Float, ease: CubicBezierEasing, alpha: Float, from: Float, to: Float): RingFrame {
        val e = ease.transform(phase(t, delay, duration))
        return RingFrame(alpha = mix(alpha, 0f, e), radius = 3f * mix(from, to, e))
    }
    return BoomLogoFrame(
        jetX = 60f * (1f - JetEase.transform(phase(t, 0f, 360f))),
        flyAlpha = keys(fly, 0f to 0.95f, 0.65f to 0.85f, 1f to 0f),
        streakScaleX = keys(fly, 0f to 1f, 1f to 0.15f),
        dotScaleX = keys(charge, 0f to 1.9f, 0.18f to 0.86f, 0.3f to 1f, 0.7f to 1.14f, 1f to 1f),
        dotScaleY = keys(charge, 0f to 0.6f, 0.18f to 1.16f, 0.3f to 1f, 0.7f to 1.14f, 1f to 1f),
        shakeX = keys(shake, 0f to 0f, 0.2f to -2.4f, 0.4f to 2.2f, 0.6f to -1.4f, 0.8f to 0.8f, 1f to 0f, easing = LinearEasing),
        shakeY = keys(shake, 0f to 0f, 0.2f to 1.2f, 0.4f to -1.2f, 0.6f to -0.6f, 0.8f to 0.6f, 1f to 0f, easing = LinearEasing),
        flashAlpha = mix(0.6f, 0f, flash),
        flashRadius = 12f * mix(0.3f, 4f, flash),
        rings = listOf(
            ring(720f, 900f, RingEaseA, 0.95f, 0.6f, 13f),
            ring(840f, 1000f, RingEaseB, 0.8f, 0.5f, 14f),
            ring(980f, 1000f, RingEaseB, 0.8f, 0.5f, 14f),
        ),
        coneAlpha = keys(cone, 0f to 0f, 0.25f to 0.9f, 1f to 0f),
        coneFraction = ConeEase.transform(cone),
        arc1Alpha = keys(arc1, 0f to 0f, 0.3f to 1f, 1f to 1f),
        arc1Scale = mix(0.2f, 1f, ArcEase.transform(arc1)),
        arc2Alpha = keys(arc2, 0f to 0f, 0.3f to 1f, 1f to 1f),
        arc2Scale = mix(0.2f, 1f, ArcEase.transform(arc2)),
    )
}

/** Arco de círculo de raio [r] indo de (x0, y0) a (x1, y1) e abaulado para a direita. */
private fun arcPath(x0: Float, y0: Float, x1: Float, y1: Float, r: Float): Path {
    val chord = y1 - y0
    val cx = x0 - sqrt(r * r - (chord / 2f) * (chord / 2f))
    val cy = (y0 + y1) / 2f
    val start = Math.toDegrees(atan2((y0 - cy).toDouble(), (x0 - cx).toDouble())).toFloat()
    val sweep = Math.toDegrees(2.0 * asin((chord / 2f / r).toDouble())).toFloat()
    return Path().apply { arcTo(Rect(cx - r, cy - r, cx + r, cy + r), start, sweep, true) }
}

private val Pivot = Offset(40f, 54f)

/**
 * O ícone do app com a animação "Estrondo atrasado": toca uma vez ao entrar e
 * de novo a cada toque. Com a animação do sistema desligada, já aparece pronto.
 */
@Composable
fun BoomLogo(
    size: Dp,
    modifier: Modifier = Modifier,
    clickLabel: String? = null,
) {
    val context = LocalContext.current
    var replay by remember { mutableIntStateOf(0) }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(replay) {
        val scale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        if (scale == 0f) {
            progress.snapTo(BoomLogoMs)
        } else {
            progress.snapTo(0f)
            // Ao entrar na tela, espera a transição de tela acabar antes de começar.
            if (replay == 0) delay(EntryDelayMs)
            progress.animateTo(BoomLogoMs, tween(BoomLogoMs.toInt(), easing = LinearEasing))
        }
    }
    val inner = remember { arcPath(56f, 40f, 56f, 68f, 20f) }
    val outer = remember { arcPath(66f, 32f, 66f, 76f, 32f) }
    val circle = remember { Path().apply { addOval(Rect(21f, 21f, 87f, 87f)) } }
    Canvas(
        modifier = modifier
            .size(size)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClickLabel = clickLabel) { replay++ },
    ) {
        val frame = boomLogoFrame(progress.value)
        val s = this.size.width / 66f
        withTransform({
            scale(s, s, Offset.Zero)
            translate(-21f, -21f)
        }) {
            withTransform({ translate(frame.shakeX, frame.shakeY) }) {
                clipPath(circle) {
                    drawCircle(IconBase, radius = 33f, center = Offset(54f, 54f))
                    if (frame.flashAlpha > 0f && progress.value >= 720f) {
                        drawCircle(IconWhite, radius = frame.flashRadius, center = Pivot, alpha = frame.flashAlpha)
                    }
                    val ringStroke = Stroke(width = 1.5.dp.toPx() / s)
                    frame.rings.forEachIndexed { i, ring ->
                        if (ring.alpha > 0f && progress.value >= RingStarts[i]) {
                            drawCircle(if (i == 2) IconPeach else IconWhite, radius = ring.radius, center = Pivot, alpha = ring.alpha, style = ringStroke)
                        }
                    }
                    if (frame.coneAlpha > 0f) {
                        drawCone(Offset(74f, 34f), IconWhite, frame)
                        drawCone(Offset(74f, 74f), IconPeach, frame)
                    }
                }
                drawSoundArc(inner, IconWhite, frame.arc1Scale, frame.arc1Alpha)
                drawSoundArc(outer, IconPeach, frame.arc2Scale, frame.arc2Alpha)
                withTransform({ translate(frame.jetX, 0f) }) {
                    if (frame.flyAlpha > 0f) drawWake(frame)
                    withTransform({ scale(frame.dotScaleX, frame.dotScaleY, Pivot) }) {
                        drawCircle(IconWhite, radius = 8f, center = Pivot)
                    }
                }
            }
        }
    }
}

private val RingStarts = floatArrayOf(720f, 840f, 980f)

private fun DrawScope.drawCone(end: Offset, color: Color, frame: BoomLogoFrame) {
    val start = Offset(42f, 54f)
    drawLine(
        color = color,
        start = start,
        end = start + (end - start) * frame.coneFraction,
        strokeWidth = 1.6f,
        cap = StrokeCap.Round,
        alpha = frame.coneAlpha,
    )
}

private fun DrawScope.drawSoundArc(path: Path, color: Color, scale: Float, alpha: Float) {
    if (alpha <= 0f) return
    withTransform({ scale(scale, scale, Pivot) }) {
        drawPath(path, color, alpha = alpha, style = Stroke(width = 5f, cap = StrokeCap.Round))
    }
}

/** O rastro do caça: linhas de velocidade e o V da onda, atrás (à direita) da bolinha. */
private fun DrawScope.drawWake(frame: BoomLogoFrame) {
    val streak = Brush.horizontalGradient(
        colors = listOf(IconWhite.copy(alpha = 0.9f), IconWhite.copy(alpha = 0f)),
        startX = 46f,
        endX = 102f,
    )
    withTransform({ scale(frame.streakScaleX, 1f, Pivot) }) {
        drawLine(streak, Offset(46f, 50f), Offset(94f, 50f), strokeWidth = 1.4f, cap = StrokeCap.Round, alpha = frame.flyAlpha)
        drawLine(streak, Offset(48f, 54f), Offset(102f, 54f), strokeWidth = 1.4f, cap = StrokeCap.Round, alpha = frame.flyAlpha)
        drawLine(streak, Offset(46f, 58f), Offset(94f, 58f), strokeWidth = 1.4f, cap = StrokeCap.Round, alpha = frame.flyAlpha)
        drawLine(IconWhite, Offset(43f, 54f), Offset(73f, 38f), strokeWidth = 1.2f, cap = StrokeCap.Round, alpha = frame.flyAlpha)
        drawLine(IconWhite, Offset(43f, 54f), Offset(73f, 70f), strokeWidth = 1.2f, cap = StrokeCap.Round, alpha = frame.flyAlpha)
    }
}
