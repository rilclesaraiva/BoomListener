// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Cores do Wrapped. Ficam fixas (não seguem a cor dinâmica nem o tema claro/escuro):
 * os cartões são uma peça de marca, com tons vivos mas não neon, e sempre escuros
 * o bastante para o texto claro por cima. Cada cartão é um par fundo/texto.
 */
data class WrappedCardColors(
    val container: Color,
    val onContainer: Color,
    /** Destaque dentro do cartão (números grandes, título). */
    val accent: Color,
)

/** Bloco colorido (gêneros): sempre um par preenchimento/texto. */
data class WrappedBlockColors(val fill: Color, val onFill: Color)

object WrappedColors {
    /** Cartões em sequência: vizinhos sempre diferem. */
    val cards: List<WrappedCardColors> = listOf(
        WrappedCardColors(Color(0xFF4A2FBD), Color(0xFFF3EEFF), Color(0xFFFFC2E0)), // violeta
        WrappedCardColors(Color(0xFFB8326B), Color(0xFFFFEEF4), Color(0xFFFFD98A)), // magenta
        WrappedCardColors(Color(0xFF0B7A75), Color(0xFFE6FFFB), Color(0xFFFFE08A)), // verde-água
        WrappedCardColors(Color(0xFFD1501B), Color(0xFFFFF3EA), Color(0xFFFFE3A3)), // laranja
        WrappedCardColors(Color(0xFF2457C5), Color(0xFFEAF1FF), Color(0xFFBFF0D4)), // azul
        WrappedCardColors(Color(0xFF1F8A4C), Color(0xFFEAFFF2), Color(0xFFFFF0A6)), // verde
    )

    val blocks: List<WrappedBlockColors> = listOf(
        WrappedBlockColors(Color(0xFFFFC2E0), Color(0xFF3A0F2A)),
        WrappedBlockColors(Color(0xFFBFF0D4), Color(0xFF06331C)),
        WrappedBlockColors(Color(0xFFFFE08A), Color(0xFF3D2A00)),
        WrappedBlockColors(Color(0xFFBFD4FF), Color(0xFF0B1F4D)),
        WrappedBlockColors(Color(0xFFE0C8FF), Color(0xFF2A1060)),
    )
}
