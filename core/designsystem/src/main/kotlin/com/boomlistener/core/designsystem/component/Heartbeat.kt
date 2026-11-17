// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.boomlistener.core.designsystem.theme.boomFastSpatialSpec
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private const val BeatScale = 1.4f
private const val BeatLiftDp = 8f
private const val Beats = 2

/**
 * O "tum tum" do coração ao favoritar: cresce, sobe um pouco e volta, **duas vezes** seguidas.
 * Dispara quando [trigger] muda para um valor maior que zero (use um contador; 0 = parado).
 */
@Composable
fun Modifier.heartbeat(trigger: Int): Modifier {
    val scale = remember { Animatable(1f) }
    val lift = remember { Animatable(0f) }
    val spec = boomFastSpatialSpec<Float>()
    LaunchedEffect(trigger) {
        if (trigger <= 0) return@LaunchedEffect
        repeat(Beats) {
            coroutineScope {
                val up = async { scale.animateTo(BeatScale, spec) }
                val rise = async { lift.animateTo(-BeatLiftDp, spec) }
                up.await(); rise.await()
            }
            coroutineScope {
                val down = async { scale.animateTo(1f, spec) }
                val fall = async { lift.animateTo(0f, spec) }
                down.await(); fall.await()
            }
        }
    }
    return this.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
        translationY = lift.value * density
    }
}
