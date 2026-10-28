// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.boomlistener.core.designsystem.theme.boomScreenSpec

/*
 * Desfoque do fundo (Ajustes → Aparência, ligado por padrão): a tela que fica
 * por baixo quando outra entra por cima (uma tela nova, o player abrindo)
 * vai desfocando, e volta nítida quando a de cima sai. O Android só desfoca
 * a partir do 12; antes disso, nada muda.
 */

/**
 * O estado da navegação que o desfoque precisa saber: se está ligado e se o
 * movimento atual é um "voltar" (a tela de baixo é a que entra) ou uma troca
 * de aba (nenhuma fica por baixo). Quem navega (o `:app`) atualiza.
 */
@Stable
class BackgroundBlurState {
    var enabled by mutableStateOf(true)
    var goingBack by mutableStateOf(false)
    var tabSwitch by mutableStateOf(false)
}

val LocalBackgroundBlur = staticCompositionLocalOf<BackgroundBlurState?> { null }

/** Desfoque máximo da tela de baixo na navegação. */
private val ScreenBlurRadius = 16.dp

/** Desfoque máximo do app por trás do player aberto. */
val PlayerBlurRadius = 24.dp

/**
 * Uma tela da navegação: desfoca quando fica por baixo de outra. [scope] é o
 * da própria tela (o `composable { }` do NavHost).
 */
@Composable
fun BoomNavScreen(scope: AnimatedVisibilityScope, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().recedeBlur(scope), propagateMinConstraints = true) {
        content()
    }
}

@Composable
private fun Modifier.recedeBlur(scope: AnimatedVisibilityScope): Modifier {
    val blur = LocalBackgroundBlur.current ?: return this
    // Abrindo: a que sai fica por baixo. Voltando: a que entra estava por baixo.
    val amount by scope.transition.animateFloat(transitionSpec = { boomScreenSpec() }, label = "recedeBlur") { state ->
        val underneath = when (state) {
            EnterExitState.PreEnter -> blur.goingBack
            EnterExitState.PostExit -> !blur.goingBack
            EnterExitState.Visible -> false
        }
        if (blur.enabled && underneath && !blur.tabSwitch) 1f else 0f
    }
    return graphicsLayer {
        val radius = ScreenBlurRadius.toPx() * amount
        renderEffect = if (radius >= 0.5f) BlurEffect(radius, radius, TileMode.Decal) else null
    }
}

/**
 * Desfoque proporcional a [fraction] (0 = nítido, 1 = [radius]), lido só na
 * hora de desenhar: acompanha o dedo sem recompor a tela.
 */
fun Modifier.fractionBlur(enabled: Boolean, fraction: () -> Float, radius: Dp = PlayerBlurRadius): Modifier =
    if (!enabled) this else graphicsLayer {
        val f = fraction().coerceIn(0f, 1f)
        val px = radius.toPx() * f
        // Todo coberto (f = 1): nada a desfocar, poupa a bateria enquanto o player está aberto.
        renderEffect = if (px >= 0.5f && f < 1f) BlurEffect(px, px, TileMode.Decal) else null
    }
