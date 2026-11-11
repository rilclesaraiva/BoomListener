// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationLabelTest {
    @Test
    fun minutosComSegundosDeDoisDigitos() {
        assertEquals("1:00", formatMinutesSeconds(60))
        assertEquals("1:01", formatMinutesSeconds(61))
        assertEquals("1:40", formatMinutesSeconds(100))
        assertEquals("2:00", formatMinutesSeconds(120))
    }
}
