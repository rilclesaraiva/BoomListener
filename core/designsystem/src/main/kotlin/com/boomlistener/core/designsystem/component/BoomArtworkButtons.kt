// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.boomlistener.core.designsystem.theme.boomEffectsSpec

/**
 * Cores dos botões do player tiradas da capa: [strong] para o botão principal
 * (tocar/pausar, aleatório e repetir ligados) e [soft] para os de apoio
 * (anterior, próxima), cada uma com a cor do que vai por cima.
 */
data class ArtworkButtonColors(
    val strong: Color,
    val onStrong: Color,
    val soft: Color,
    val onSoft: Color,
)

/**
 * Tons da capa [seed] para os botões, sobre um fundo escuro ([dark]) ou claro.
 * O principal fica bem destacado do fundo (claro no tema escuro, escuro no
 * claro), o de apoio fica perto do fundo, e o que vai por cima de cada um
 * tem contraste de leitura (pelo menos 4,5:1).
 */
fun artworkButtonColors(seed: Color, dark: Boolean): ArtworkButtonColors {
    val color = seed.copy(alpha = 1f)
    val strong = if (dark) {
        mixUntil(color, Color.White) { it.luminance() >= STRONG_MIN_ON_DARK }
    } else {
        mixUntil(color, Color.Black) { it.luminance() <= STRONG_MAX_ON_LIGHT }
    }
    val soft = if (dark) {
        mixUntil(lerp(color, Color.Black, 0.45f), Color.Black) { it.luminance() <= SOFT_MAX_ON_DARK }
    } else {
        mixUntil(lerp(color, Color.White, 0.6f), Color.White) { it.luminance() >= SOFT_MIN_ON_LIGHT }
    }
    // Por cima do de apoio: um tom da própria capa, se ler bem; senão branco ou preto.
    val tintedInk = if (dark) lerp(color, Color.White, 0.8f) else lerp(color, Color.Black, 0.75f)
    val onSoft = if (contrast(tintedInk, soft) >= MIN_CONTRAST) tintedInk else inkFor(soft)
    return ArtworkButtonColors(strong = strong, onStrong = inkFor(strong), soft = soft, onSoft = onSoft)
}

/** Contraste entre duas cores (1 a 21), como nas regras de acessibilidade. */
fun contrast(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    return (maxOf(la, lb) + 0.05f) / (minOf(la, lb) + 0.05f)
}

/** Branco ou preto, o que ler melhor sobre [background]. */
private fun inkFor(background: Color): Color =
    if (contrast(Color.White, background) >= contrast(Color.Black, background)) Color.White else Color.Black

/** Mistura [color] com [target] aos poucos, até [done] (ou chegar em [target]). */
private fun mixUntil(color: Color, target: Color, done: (Color) -> Boolean): Color {
    var step = 0
    var mixed = color
    while (!done(mixed) && step < MIX_STEPS) {
        step++
        mixed = lerp(color, target, step / MIX_STEPS.toFloat())
    }
    return mixed
}

private const val MIX_STEPS = 20
private const val MIN_CONTRAST = 4.5f
private const val STRONG_MIN_ON_DARK = 0.35f
private const val STRONG_MAX_ON_LIGHT = 0.17f
private const val SOFT_MAX_ON_DARK = 0.06f
private const val SOFT_MIN_ON_LIGHT = 0.6f

/**
 * Os botões de dentro ([content]) na cor da capa [seed], quando [enabled]:
 * troca só as cores de botão do tema (primary e secondaryContainer, com as
 * "on"), e a troca entre músicas é animada. Desligado ou sem capa, o tema
 * fica como está.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BoomArtworkButtons(seed: Color?, enabled: Boolean, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val target = if (enabled && seed != null) {
        artworkButtonColors(seed, dark = scheme.surface.luminance() < 0.5f)
    } else {
        ArtworkButtonColors(scheme.primary, scheme.onPrimary, scheme.secondaryContainer, scheme.onSecondaryContainer)
    }
    val strong by animateColorAsState(target.strong, boomEffectsSpec(), label = "artworkStrong")
    val onStrong by animateColorAsState(target.onStrong, boomEffectsSpec(), label = "artworkOnStrong")
    val soft by animateColorAsState(target.soft, boomEffectsSpec(), label = "artworkSoft")
    val onSoft by animateColorAsState(target.onSoft, boomEffectsSpec(), label = "artworkOnSoft")
    MaterialExpressiveTheme(
        colorScheme = scheme.copy(
            primary = strong,
            onPrimary = onStrong,
            secondaryContainer = soft,
            onSecondaryContainer = onSoft,
        ),
        motionScheme = MaterialTheme.motionScheme,
        shapes = MaterialTheme.shapes,
        typography = MaterialTheme.typography,
        content = content,
    )
}
