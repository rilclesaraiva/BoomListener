// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.boomlistener.core.designsystem.theme.boomSpatialSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Estado do painel do player, que desliza de baixo para cima por cima do app.
 *
 * [progress] vai de 0 (fechado, só o mini player) a 1 (tela cheia). Os gestos
 * mexem nele diretamente e, ao soltar o dedo, ele termina o movimento com a
 * mola do motionScheme.
 */
@Stable
class PlayerSheetState internal constructor(
    private val scope: CoroutineScope,
    private val spec: FiniteAnimationSpec<Float>,
) {
    internal val progress = Animatable(0f)

    /** Altura da tela em px, informada pelo próprio painel ao se medir. */
    internal var screenHeightPx: Float = 1f

    /** true quando o painel está aberto ou abrindo (alvo da animação). */
    val isExpanded: Boolean
        get() = progress.targetValue > 0.5f

    fun expand() {
        scope.launch { progress.animateTo(1f, spec) }
    }

    fun collapse() {
        scope.launch { progress.animateTo(0f, spec) }
    }

    /** Arraste: [deltaPx] positivo puxa para cima (abre). */
    internal fun dragBy(deltaPx: Float) {
        val next = (progress.value + deltaPx / screenHeightPx).coerceIn(0f, 1f)
        scope.launch { progress.snapTo(next) }
    }

    /** Segue o dedo durante o gesto de voltar do sistema. */
    internal fun snapTo(value: Float) {
        scope.launch { progress.snapTo(value.coerceIn(0f, 1f)) }
    }

    /**
     * Soltou o dedo: decide abrir ou fechar pela velocidade (px/s, positiva
     * para cima) ou, se o gesto foi lento, por qual lado está mais perto.
     */
    internal fun settle(velocityUp: Float) {
        val open = when {
            velocityUp > FLING_VELOCITY -> true
            velocityUp < -FLING_VELOCITY -> false
            else -> progress.value > 0.5f
        }
        if (open) expand() else collapse()
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
