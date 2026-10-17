// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.palette.graphics.Palette
import com.boomlistener.core.designsystem.theme.boomEffectsSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Cor principal de uma capa (vibrante se tiver, senão a dominante). null sem capa. */
@Composable
fun rememberArtworkColor(image: ImageBitmap?): Color? {
    val color by produceState<Color?>(initialValue = null, image) {
        value = image?.let {
            withContext(Dispatchers.Default) {
                val palette = Palette.from(it.asAndroidBitmap()).maximumColorCount(16).generate()
                val swatch = palette.vibrantSwatch ?: palette.darkVibrantSwatch ?: palette.dominantSwatch
                swatch?.rgb?.let(::Color)
            }
        }
    }
    return color
}

/**
 * Fundo tingido pela capa: a cor da capa misturada à superfície do tema no
 * topo, descendo até a superfície pura. A mistura mantém o contraste dos
 * textos do tema; a troca de cor entre músicas é animada.
 */
@Composable
fun Modifier.artworkTintedBackground(seed: Color?): Modifier {
    val surface = MaterialTheme.colorScheme.surface
    val target = seed?.let { lerp(surface, it, 0.42f) } ?: surface
    val top by animateColorAsState(target, animationSpec = boomEffectsSpec(), label = "artworkTint")
    return drawBehind {
        drawRect(Brush.verticalGradient(0f to top, 0.75f to lerp(surface, top, 0.35f), 1f to surface))
    }
}

/** Tom pastel da capa e a cor de texto com bom contraste sobre ele. */
private fun pastelOf(seed: Color): Pair<Color, Color> {
    val pastel = lerp(seed, Color.White, 0.55f)
    val onPastel = if (pastel.luminance() > 0.45f) Color(0xFF1B1B1F) else Color.White
    return pastel to onPastel
}

/**
 * Superfície no tom pastel da capa (gradiente suave da esquerda para a
 * direita), com a cor do conteúdo ajustada para manter o contraste. Sem
 * capa, usa o primaryContainer do tema. A troca entre músicas é animada.
 */
@Composable
fun BoomArtworkSurface(
    seed: Color?,
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val fallback = MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    val (targetContainer, targetContent) = seed?.let(::pastelOf) ?: fallback
    val container by animateColorAsState(targetContainer, animationSpec = boomEffectsSpec(), label = "pastel")
    val onContainer by animateColorAsState(targetContent, animationSpec = boomEffectsSpec(), label = "onPastel")
    val deeper = lerp(container, seed ?: container, 0.25f)
    Box(
        modifier
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(container, deeper))),
    ) {
        CompositionLocalProvider(LocalContentColor provides onContainer, content = content)
    }
}
