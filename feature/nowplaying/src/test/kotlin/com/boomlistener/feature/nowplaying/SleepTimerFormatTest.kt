// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import org.junit.Assert.assertEquals
import org.junit.Test

class SleepTimerFormatTest {
    @Test
    fun contagemArredondaParaCima() {
        assertEquals("00:01", formatCountdown(1))
        assertEquals("00:00", formatCountdown(0))
        assertEquals("00:00", formatCountdown(-500))
        assertEquals("15:00", formatCountdown(15 * 60_000L))
        assertEquals("1:05:09", formatCountdown((3600 + 5 * 60 + 9) * 1000L))
    }

    @Test
    fun personalizadoSomaHorasEMinutos() {
        assertEquals(0, customSleepMinutes("", ""))
        assertEquals(90, customSleepMinutes("1", "30"))
        assertEquals(25, customSleepMinutes("", "25"))
        assertEquals(23 * 60 + 59, customSleepMinutes("99", "99"))
        assertEquals(60, customSleepMinutes("1a", "x"))
    }
}
