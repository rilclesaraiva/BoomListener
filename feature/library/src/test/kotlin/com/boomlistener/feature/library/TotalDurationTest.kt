// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import org.junit.Assert.assertEquals
import org.junit.Test

class TotalDurationTest {
    @Test
    fun albumDeMenosDeUmaHora() {
        assertEquals(0L to 42L, hoursAndMinutes(42 * 60_000L))
    }

    @Test
    fun albumComMaisDeUmaHora() {
        assertEquals(1L to 5L, hoursAndMinutes(65 * 60_000L))
        assertEquals(2L to 0L, hoursAndMinutes(120 * 60_000L))
    }

    @Test
    fun segundosQueSobramArredondamParaCima() {
        assertEquals(0L to 1L, hoursAndMinutes(40_000L))
        assertEquals(1L to 0L, hoursAndMinutes(59 * 60_000L + 30_000L))
    }

    @Test
    fun vazioEZero() {
        assertEquals(0L to 0L, hoursAndMinutes(0L))
        assertEquals(0L to 0L, hoursAndMinutes(-5L))
    }
}
