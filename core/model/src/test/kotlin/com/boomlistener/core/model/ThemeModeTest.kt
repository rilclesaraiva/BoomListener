// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/boomlistener/blob/main/LICENSE

package com.boomlistener.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {
    @Test
    fun `existem os quatro temas previstos`() {
        assertEquals(
            listOf("System", "Light", "Dark", "Amoled"),
            ThemeMode.entries.map { it.name },
        )
    }
}
