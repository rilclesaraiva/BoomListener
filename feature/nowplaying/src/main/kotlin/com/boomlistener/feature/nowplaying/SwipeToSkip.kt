// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import com.boomlistener.core.designsystem.theme.boomFastSpatialSpec
import kotlin.math.abs

/** Quanto da largura o dedo precisa andar para trocar de música. */
private const val SKIP_FRACTION = 0.3f

/** Arremesso rápido (px/s) que troca mesmo sem andar tanto. */
private const val SKIP_VELOCITY = 1_200f

/**
 * Deslizar para os lados troca de música: para a esquerda, a próxima; para a
 * direita, a anterior. O conteúdo acompanha o dedo; passando do ponto, sai
 * pelo lado e a música nova entra pelo outro. Usado no mini player e na capa
 * do player aberto.
 */
@Composable
internal fun Modifier.swipeToSkip(onPrevious: () -> Unit, onNext: () -> Unit): Modifier {
    var offset by remember { mutableFloatStateOf(0f) }
    var width by remember { mutableFloatStateOf(1f) }
    val spec = boomFastSpatialSpec<Float>()
    val haptics = LocalHapticFeedback.current
    val dragState = rememberDraggableState { delta -> offset += delta }
    return this
        .onSizeChanged { width = it.width.toFloat().coerceAtLeast(1f) }
        .draggable(
            state = dragState,
            orientation = Orientation.Horizontal,
            onDragStopped = { velocity ->
                val next = offset < -width * SKIP_FRACTION || velocity < -SKIP_VELOCITY
                val previous = offset > width * SKIP_FRACTION || velocity > SKIP_VELOCITY
                if (next || previous) {
                    haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
                    val out = if (next) -width else width
                    animate(offset, out, animationSpec = spec) { value, _ -> offset = value }
                    if (next) onNext() else onPrevious()
                    // A nova entra pelo lado oposto.
                    offset = -out * 0.4f
                }
                animate(offset, 0f, animationSpec = spec) { value, _ -> offset = value }
            },
        )
        .graphicsLayer {
            translationX = offset
            alpha = 1f - (abs(offset) / width).coerceIn(0f, 1f) * 0.7f
        }
}
