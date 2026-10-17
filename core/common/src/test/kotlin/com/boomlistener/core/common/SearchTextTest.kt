// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchTextTest {
    @Test
    fun removeAcentosEMaiusculas() {
        assertEquals("joao gilberto", "João Gilberto".normalizedForSearch())
        assertEquals("acai", "AÇAÍ".normalizedForSearch())
        assertEquals("beyonce", "  Beyoncé ".normalizedForSearch())
    }
}
