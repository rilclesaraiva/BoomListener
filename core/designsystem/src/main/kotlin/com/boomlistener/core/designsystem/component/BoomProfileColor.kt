// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private const val PROFILE_TOP_ALPHA = 0.55f

/**
 * Cores prontas para o perfil (a pessoa também pode digitar um hexadecimal).
 * É a cor escolhida pelo usuário, não do tema; mora aqui, no design system.
 */
val BoomProfilePalette: List<String> = listOf(
    "#E53935", "#D81B60", "#8E24AA", "#5E35B1", "#3949AB", "#1E88E5", "#039BE5",
    "#00897B", "#43A047", "#7CB342", "#FDD835", "#FB8C00", "#F4511E", "#6D4C41",
    "#546E7A", "#7A0000",
)

/** "#RRGGBB" → cor; vazio ou inválido → null. */
fun parseProfileColor(hex: String?): Color? {
    val clean = hex?.trim()?.removePrefix("#") ?: return null
    if (clean.length != 6) return null
    val value = clean.toLongOrNull(16) ?: return null
    return Color(0xFF000000 or value)
}

/**
 * Fundo do perfil na cor escolhida: forte no topo e sumindo para baixo, como
 * nos perfis do Discord, ou a mesma cor no perfil inteiro com [solid]. Sem
 * cor, não muda nada.
 */
fun Modifier.profileBackground(hex: String?, solid: Boolean = false): Modifier {
    val color = parseProfileColor(hex) ?: return this
    if (solid) return background(color)
    return background(
        Brush.verticalGradient(
            0f to color.copy(alpha = PROFILE_TOP_ALPHA),
            0.45f to color.copy(alpha = 0.18f),
            1f to Color.Transparent,
        ),
    )
}

/**
 * Perfil com cor sólida: a página é a própria cor (o #000000 fica preto
 * puro, em qualquer tema) e os textos, cartões e contornos ganham tons tirados
 * dela, claros sobre cor escura e escuros sobre cor clara, para ler bem. Sem
 * cor, ou no degradê, o tema do app fica como está.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BoomProfileSurface(hex: String?, solid: Boolean, content: @Composable () -> Unit) {
    val color = parseProfileColor(hex)
    if (!solid || color == null) {
        content()
        return
    }
    MaterialExpressiveTheme(
        colorScheme = MaterialTheme.colorScheme.onSolidColor(color),
        motionScheme = MaterialTheme.motionScheme,
        shapes = MaterialTheme.shapes,
        typography = MaterialTheme.typography,
        content = content,
    )
}

/** As superfícies do tema trocadas por tons de [color]; o resto (destaques) fica. */
internal fun ColorScheme.onSolidColor(color: Color): ColorScheme {
    val ink = if (color.luminance() > SOLID_LIGHT_LUMINANCE) Color.Black else Color.White
    fun tone(amount: Float) = lerp(color, ink, amount)
    return copy(
        background = color,
        onBackground = ink,
        surface = color,
        onSurface = ink,
        surfaceDim = color,
        surfaceBright = tone(0.12f),
        surfaceContainerLowest = tone(0.02f),
        surfaceContainerLow = tone(0.05f),
        surfaceContainer = tone(0.08f),
        surfaceContainerHigh = tone(0.11f),
        surfaceContainerHighest = tone(0.15f),
        surfaceVariant = tone(0.15f),
        onSurfaceVariant = lerp(ink, color, 0.3f),
        outline = lerp(ink, color, 0.5f),
        outlineVariant = lerp(ink, color, 0.75f),
        inverseSurface = ink,
        inverseOnSurface = color,
    )
}

/** A partir daqui a cor é clara: textos escuros. */
private const val SOLID_LIGHT_LUMINANCE = 0.4f
