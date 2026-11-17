// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryDurationTest {
    private val minute = 60_000L
    private val hour = 60 * minute

    @Test
    fun horasEMinutosInteiros() {
        assertEquals(LibraryTotalTime(10, 24), libraryTotalTime(10 * hour + 24 * minute + 40_000L))
        assertEquals(LibraryTotalTime(32, 0), libraryTotalTime(32 * hour))
        assertEquals(LibraryTotalTime(0, 59), libraryTotalTime(59 * minute + 59_000L))
    }

    @Test
    fun naoArredondaParaCima() {
        assertEquals(LibraryTotalTime(9, 59), libraryTotalTime(9 * hour + 59 * minute + 59_999L))
        assertEquals(LibraryTotalTime(1, 0), libraryTotalTime(hour))
    }

    @Test
    fun bibliotecaVaziaOuNegativaViraZero() {
        assertEquals(LibraryTotalTime(0, 0), libraryTotalTime(0L))
        assertEquals(LibraryTotalTime(0, 0), libraryTotalTime(-5L))
    }
}
