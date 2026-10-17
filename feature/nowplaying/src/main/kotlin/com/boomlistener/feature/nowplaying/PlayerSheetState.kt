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

    /** true quando o painel está aberto ou abrindo. */
    val isExpanded: Boolean
        get() = target > 0.5f

    /** true enquanto qualquer parte do painel está na tela. */
    internal val isVisible: Boolean
        get() = progress > 0f || target > 0f

    fun expand() = animateTo(1f)

    fun collapse() = animateTo(0f)

    /** Arraste: [deltaPx] positivo puxa para cima (abre). */
    internal fun dragBy(deltaPx: Float) {
        animation?.cancel()
        progress = (progress + deltaPx / screenHeightPx).coerceIn(0f, 1f)
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
