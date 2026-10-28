// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkButtonsTest {
    // Capas de todo tipo: vibrantes, quase brancas, quase pretas, cinza.
    private val seeds = listOf(
        Color(0xFFE53935), Color(0xFFFFEB3B), Color(0xFF1A237E), Color(0xFF00E676),
        Color(0xFFFAFAFA), Color(0xFF0A0A0A), Color(0xFF808080), Color(0xFF8E24AA),
    )

    @Test
    fun textoSempreLegivelNosBotoes() {
        for (seed in seeds) for (dark in listOf(true, false)) {
            val colors = artworkButtonColors(seed, dark)
            assertTrue("principal $seed dark=$dark", contrast(colors.strong, colors.onStrong) >= 4.5f)
            assertTrue("apoio $seed dark=$dark", contrast(colors.soft, colors.onSoft) >= 4.5f)
        }
    }

    @Test
    fun principalSeDestacaDoFundo() {
        for (seed in seeds) {
            assertTrue("tema escuro $seed", artworkButtonColors(seed, dark = true).strong.luminance() >= 0.35f)
            assertTrue("tema claro $seed", artworkButtonColors(seed, dark = false).strong.luminance() <= 0.17f)
        }
    }

    @Test
    fun capaQueJaServeNaoMuda() {
        // Um amarelo já é claro o bastante para o tema escuro: fica ele mesmo.
        val yellow = Color(0xFFFFEB3B)
        assertTrue(artworkButtonColors(yellow, dark = true).strong == yellow)
    }
}
