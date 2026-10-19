// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Velocidade do vai e vem: dp por segundo (devagar, dá para ler). */
private const val DP_PER_SECOND = 30f
private const val PAUSE_AT_START_MS = 1_500L
private const val PAUSE_AT_END_MS = 2_000L

/**
 * Texto de uma linha que, se não couber, vai devagar até o fim, espera ~2 s,
 * volta ao começo e repete. Se couber, fica parado.
 *
 * Exceção à regra de movimento do app: é um letreiro de velocidade constante
 * (proporcional ao tamanho do texto), não uma transição; por isso usa um
 * `tween` linear calculado aqui, e não as curvas do motionScheme.
 */
@Composable
fun BoomBouncingText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
) {
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    LaunchedEffect(text, scroll.maxValue) {
        scroll.scrollTo(0)
        val distance = scroll.maxValue
        if (distance <= 0) return@LaunchedEffect
        val pxPerSecond = with(density) { DP_PER_SECOND.dp.toPx() }
        val duration = (distance / pxPerSecond * 1000).toInt().coerceAtLeast(600)
        while (true) {
            delay(PAUSE_AT_START_MS)
            scroll.animateScrollTo(distance, tween(duration, easing = LinearEasing))
            delay(PAUSE_AT_END_MS)
            scroll.animateScrollTo(0, tween(duration, easing = LinearEasing))
        }
    }
    Text(
        text = text,
        style = style,
        color = color,
        maxLines = 1,
        softWrap = false,
        modifier = modifier.horizontalScroll(scroll, enabled = false),
    )
}
