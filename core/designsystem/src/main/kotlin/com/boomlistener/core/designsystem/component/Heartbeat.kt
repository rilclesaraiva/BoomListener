// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.boomlistener.core.designsystem.theme.boomFastSpatialSpec
import kotlin.math.PI
import kotlin.math.sin

private const val BeatBig = 1.4f
private const val BeatSmall = 1.28f

/**
 * O "tum tum" do coração ao favoritar: **expande no lugar**, duas batidas (a segunda um
 * pouco menor). Dispara quando [trigger] muda para um valor maior que zero (um contador).
 */
@Composable
fun Modifier.heartbeat(trigger: Int): Modifier {
    val scale = remember { Animatable(1f) }
    val spec = boomFastSpatialSpec<Float>()
    LaunchedEffect(trigger) {
        if (trigger <= 0) return@LaunchedEffect
        scale.animateTo(BeatBig, spec)
        scale.animateTo(1f, spec)
        scale.animateTo(BeatSmall, spec)
        scale.animateTo(1f, spec)
    }
    return this.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

// Coreografia de marca com tempos próprios (como o BoomLogo): decisão de 17/11/2026.
private const val BurstMs = 1850
private const val PulseMs = 450f
private const val BalloonMs = 1100f

private class Balloon(val startMs: Float, val dir: Float, val size: Float, val tilt: Float)

private val Balloons = listOf(
    Balloon(PulseMs - 50f, -1f, 0.30f, -14f),
    Balloon(PulseMs + 130f, 1f, 0.22f, 16f),
    Balloon(PulseMs + 290f, 0f, 0.38f, -6f),
)

private fun easeOut(t: Float): Float {
    val u = 1f - t.coerceIn(0f, 1f)
    return 1f - u * u * u
}

/** 0..1 → escala das duas batidas (suavizada entre as marcas). */
private fun beatScale(t: Float): Float {
    val marks = floatArrayOf(0f, .12f, .28f, .40f, .58f, 1f)
    val values = floatArrayOf(1f, BeatBig, 1f, BeatSmall, 1f, 1f)
    val x = t.coerceIn(0f, 1f)
    for (i in 1 until marks.size) {
        if (x <= marks[i]) {
            val u = (x - marks[i - 1]) / (marks[i] - marks[i - 1])
            val s = u * u * (3 - 2 * u)
            return values[i - 1] + (values[i] - values[i - 1]) * s
        }
    }
    return 1f
}

/**
 * Toque rápido na capa que toca: um coração grande bate duas vezes no centro (tum tum) e,
 * em seguida, três corações sobem balançando como balões, cada um para um lado, e somem
 * aos poucos. Dispara quando [trigger] muda para um valor maior que zero.
 */
@Composable
fun CoverHeartBurst(trigger: Int, modifier: Modifier = Modifier) {
    val clock = remember { Animatable(-1f) }
    LaunchedEffect(trigger) {
        if (trigger <= 0) return@LaunchedEffect
        clock.snapTo(0f)
        clock.animateTo(BurstMs.toFloat(), tween(BurstMs, easing = LinearEasing))
        clock.snapTo(-1f)
    }
    BoxWithConstraints(modifier) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val color = MaterialTheme.colorScheme.primary
        // Batida do centro.
        HeartLayer(maxWidth * 0.44f, color) {
            val ms = clock.value
            if (ms < 0f || ms > PulseMs + 140f) {
                alpha = 0f
            } else {
                val u = ms / PulseMs
                val s = (0.5f + 0.5f * easeOut(ms / 120f)) * beatScale(u)
                scaleX = s; scaleY = s
                rotationZ = -8f
                alpha = if (ms < PulseMs - 70f) 1f else 1f - ((ms - (PulseMs - 70f)) / 210f).coerceIn(0f, 1f)
            }
        }
        // Os três balões.
        Balloons.forEach { b ->
            HeartLayer(maxWidth * b.size, color) {
                val u = (clock.value - b.startMs) / BalloonMs
                if (clock.value < 0f || u <= 0f || u >= 1f) {
                    alpha = 0f
                } else {
                    translationY = -easeOut(u) * widthPx * 0.45f
                    translationX = b.dir * widthPx * (0.18f * sin(3f * PI.toFloat() * u) + 0.1f * u)
                    rotationZ = b.tilt + 12f * sin(4f * PI.toFloat() * u)
                    val s = 0.4f + 0.6f * easeOut((u * 4f).coerceAtMost(1f))
                    scaleX = s; scaleY = s
                    alpha = when {
                        u < .08f -> u / .08f
                        u < .4f -> 1f
                        else -> 1f - easeOut((u - .4f) / .6f)
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.HeartLayer(
    size: Dp,
    tint: Color,
    layer: GraphicsLayerScope.() -> Unit,
) {
    Icon(
        BoomIcons.Favorite,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.align(Alignment.Center).size(size).graphicsLayer(block = layer),
    )
}
