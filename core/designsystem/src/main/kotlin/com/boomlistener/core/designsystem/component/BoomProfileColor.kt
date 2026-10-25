// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

private const val PROFILE_TOP_ALPHA = 0.55f

/** Sólida, a cor cobre a página toda: um pouco mais leve que o topo do degradê, para o texto ler bem. */
private const val PROFILE_SOLID_ALPHA = 0.4f

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
    if (solid) return background(color.copy(alpha = PROFILE_SOLID_ALPHA))
    return background(
        Brush.verticalGradient(
            0f to color.copy(alpha = PROFILE_TOP_ALPHA),
            0.45f to color.copy(alpha = 0.18f),
            1f to Color.Transparent,
        ),
    )
}

/**
 * A cor do topo do fundo do perfil, opaca, sobre [base] (o fundo da página):
 * é a cor da barra de cima do perfil. Sem cor, null.
 */
fun profileTopColor(hex: String?, base: Color): Color? {
    val color = parseProfileColor(hex) ?: return null
    return color.copy(alpha = PROFILE_TOP_ALPHA).compositeOver(base)
}
