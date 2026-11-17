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
    // Só bate quando o contador muda depois de entrar na tela (não ao reaparecer com valor antigo).
    val seen = remember { intArrayOf(trigger) }
    LaunchedEffect(trigger) {
        if (trigger <= 0 || trigger == seen[0]) return@LaunchedEffect
        seen[0] = trigger
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
