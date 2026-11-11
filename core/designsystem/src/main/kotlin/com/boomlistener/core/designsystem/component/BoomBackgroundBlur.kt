// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
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
fun BoomNavScreen(scope: AnimatedVisibilityScope, tab: Boolean = false, content: @Composable () -> Unit) {
    // As telas das abas guardam a folga da barra de abas por conta própria, como
    // folga no fim da lista (e não encolhendo a tela): a barra some nas telas de
    // dentro e quando há músicas marcadas, e a lista da aba não pode crescer nem
    // encolher por isso (ao voltar, ela voltava rolada uma música acima).
    val inset = if (tab) LocalTabBarInset.current else 0.dp
    Box(Modifier.fillMaxSize().recedeBlur(scope), propagateMinConstraints = true) {
        CompositionLocalProvider(LocalBottomOverlay provides LocalBottomOverlay.current + inset) {
            content()
        }
    }
}

/**
 * Quanto a tela fica desfocada (0 = nítida, 1 = [ScreenBlurRadius]).
 *
 * [settled] é verdadeiro quando a tela é a de cima, parada e interativa (a
 * entrada da navegação está em RESUMED). Ela nunca desfoca: assim um "voltar"
 * preditivo começado e desistido, ou ligar e desligar o desfoque no meio, não
 * deixa a tela em que a pessoa está presa desfocada.
 */
internal fun recedeTarget(
    enabled: Boolean,
    settled: Boolean,
    state: EnterExitState,
    goingBack: Boolean,
    tabSwitch: Boolean,
): Float {
    if (!enabled || settled || tabSwitch) return 0f
    // Abrindo: a que sai fica por baixo. Voltando: a que entra estava por baixo.
    val underneath = when (state) {
        EnterExitState.PreEnter -> goingBack
        EnterExitState.PostExit -> !goingBack
        EnterExitState.Visible -> false
    }
    return if (underneath) 1f else 0f
}

@Composable
private fun Modifier.recedeBlur(scope: AnimatedVisibilityScope): Modifier {
    val blur = LocalBackgroundBlur.current ?: return this
    // A entrada da navegação só fica em RESUMED quando já terminou de entrar (e não está saindo).
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val settled = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    // A animação existe sempre (ligar ou desligar o desfoque só muda o alvo): criá-la ou
    // tirá-la no meio de um movimento deixava o valor num estado que não bate com a tela.
    val amount by scope.transition.animateFloat(transitionSpec = { boomScreenSpec() }, label = "recedeBlur") { state ->
        recedeTarget(blur.enabled, settled, state, blur.goingBack, blur.tabSwitch)
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
