// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
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
 * O tom do topo do player: a cor da capa misturada à superfície do tema. É a
 * mesma cor do mini player, para os dois combinarem quando um vira o outro.
 */
@Composable
fun artworkTopTint(seed: Color?): Color {
    val surface = MaterialTheme.colorScheme.surface
    return seed?.let { lerp(surface, it, TINT_AMOUNT) } ?: surface
}

private const val TINT_AMOUNT = 0.42f

/**
 * Fundo tingido pela capa: a cor da capa misturada à superfície do tema no
 * topo, descendo até a superfície pura. A mistura mantém o contraste dos
 * textos do tema; a troca de cor entre músicas é animada.
 */
@Composable
fun Modifier.artworkTintedBackground(seed: Color?): Modifier {
    val surface = MaterialTheme.colorScheme.surface
    val target = artworkTopTint(seed)
    val top by animateColorAsState(target, animationSpec = boomEffectsSpec(), label = "artworkTint")
    return drawBehind {
        drawRect(Brush.verticalGradient(0f to top, 0.75f to lerp(surface, top, 0.35f), 1f to surface))
    }
}

/**
 * Cor de destaque sobre o pastel (barra de progresso, por exemplo): a própria
 * cor da capa, escurecida o bastante para aparecer sobre o pastel claro, mas
 * nunca preto puro. Sobre um pastel escuro, um tom bem claro da capa.
 */
private fun accentOn(pastel: Color, seed: Color): Color =
    if (pastel.luminance() > 0.45f) {
        lerp(seed, Color.Black, 0.35f).let { if (it.luminance() < 0.02f) lerp(seed, Color.Black, 0.15f) else it }
    } else {
        lerp(seed, Color.White, 0.8f)
    }

/**
 * Cor de destaque dentro de [BoomArtworkSurface] (tom da capa com bom
 * contraste sobre o pastel). Fora dela, a cor primária do tema.
 */
val LocalArtworkAccent = staticCompositionLocalOf { Color.Unspecified }

/**
 * Superfície no tom do topo do player ([artworkTopTint]): o mini player tem a
 * mesma cor do player aberto. Sem capa, a superfície do tema. A troca entre
 * músicas é animada.
 */
@Composable
fun BoomArtworkSurface(
    seed: Color?,
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val targetContainer = if (seed != null) artworkTopTint(seed) else MaterialTheme.colorScheme.surfaceContainerHigh
    val targetContent = MaterialTheme.colorScheme.onSurface
    val targetAccent = seed?.let { accentOn(targetContainer, it) } ?: MaterialTheme.colorScheme.primary
    val container by animateColorAsState(targetContainer, animationSpec = boomEffectsSpec(), label = "miniTint")
    val onContainer by animateColorAsState(targetContent, animationSpec = boomEffectsSpec(), label = "onMiniTint")
    val accent by animateColorAsState(targetAccent, animationSpec = boomEffectsSpec(), label = "accent")
    Box(
        modifier
            .clip(shape)
            .background(container),
    ) {
        CompositionLocalProvider(
            LocalContentColor provides onContainer,
            LocalArtworkAccent provides accent,
            content = content,
        )
    }
}
