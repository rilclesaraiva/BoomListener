// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import com.boomlistener.core.designsystem.theme.boomSpatialSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Estado do painel do player, que desliza de baixo para cima por cima do app.
 *
 * [progress] vai de 0 (fechado, só o mini player) a 1 (tela cheia). Durante um
 * gesto ele muda na hora (sem corrotina, para nunca ficar fora de ordem); ao
 * soltar o dedo, uma única animação com a mola do motionScheme termina o
 * movimento, e qualquer gesto novo a interrompe.
 */
@Stable
class PlayerSheetState internal constructor(
    private val scope: CoroutineScope,
    private val spec: FiniteAnimationSpec<Float>,
) {
    internal var progress by mutableFloatStateOf(0f)
        private set

    /** Para onde o painel está indo: 1 = aberto, 0 = fechado. */
    private var target by mutableFloatStateOf(0f)

    private var animation: Job? = null

    /** Altura da tela em px, informada pelo próprio painel ao se medir. */
    internal var screenHeightPx: Float = 1f

    /**
     * Onde fica o topo do mini player (px a partir do topo da tela), informado
     * pelo :app com [playerSheetAnchor]. O player cresce a partir dali: fechado
     * ele é o mini player, aberto ocupa a tela. 0 = ainda não medido.
     */
    internal var collapsedTopPx by mutableFloatStateOf(0f)

    /** Onde fica a base do mini player (px a partir do topo); 0 = não medido. */
    internal var collapsedBottomPx by mutableFloatStateOf(0f)

    /** Quanto o painel anda entre fechado e aberto (o dedo segue essa distância). */
    internal val travelPx: Float
        get() = if (collapsedTopPx > 0f) collapsedTopPx else screenHeightPx

    /** 0 = fechado (mini player), 1 = aberto. Para quem acompanha a abertura (a barra de abas desce junto). */
    val fraction: Float
        get() = progress.coerceIn(0f, 1f)

    /** true quando o painel está aberto ou abrindo. */
    val isExpanded: Boolean
        get() = target > 0.5f

    /** true enquanto qualquer parte do painel está na tela. */
    internal val isVisible: Boolean
        get() = progress > 0f || target > 0f

    fun expand() = animateTo(1f)

    fun collapse() = animateTo(0f)

    /** Fecha na hora, sem animação (ex.: a fila, quando o player some). */
    internal fun reset() {
        animation?.cancel()
        target = 0f
        progress = 0f
    }

    /** Arraste: [deltaPx] positivo puxa para cima (abre). */
    internal fun dragBy(deltaPx: Float) {
        animation?.cancel()
        progress = (progress + deltaPx / travelPx.coerceAtLeast(1f)).coerceIn(0f, 1f)
    }

    /** Segue o dedo durante o gesto de voltar do sistema. */
    internal fun snapTo(value: Float) {
        animation?.cancel()
        progress = value.coerceIn(0f, 1f)
    }

    /**
     * Soltou o dedo: decide abrir ou fechar pela velocidade (px/s, positiva
     * para cima) ou, se o gesto foi lento, por qual lado está mais perto.
     */
    internal fun settle(velocityUp: Float) {
        val open = when {
            velocityUp > FLING_VELOCITY -> true
            velocityUp < -FLING_VELOCITY -> false
            else -> progress > 0.5f
        }
        if (open) expand() else collapse()
    }

    private fun animateTo(value: Float) {
        animation?.cancel()
        target = value
        animation = scope.launch {
            animate(initialValue = progress, targetValue = value, animationSpec = spec) { current, _ ->
                progress = current
            }
        }
    }

    private companion object {
        const val FLING_VELOCITY = 1200f
    }
}

@Composable
fun rememberPlayerSheetState(): PlayerSheetState {
    val scope = rememberCoroutineScope()
    val spec = boomSpatialSpec<Float>()
    return remember { PlayerSheetState(scope, spec) }
}

/**
 * Marca o mini player como o ponto de onde o player em tela cheia cresce.
 * Usado no :app, em volta do mini player.
 */
fun Modifier.playerSheetAnchor(state: PlayerSheetState): Modifier =
    onGloballyPositioned { coordinates ->
        val top = coordinates.positionInRoot().y
        if (coordinates.size.height > 0 && top > 0f) {
            state.collapsedTopPx = top
            state.collapsedBottomPx = top + coordinates.size.height
        }
    }
