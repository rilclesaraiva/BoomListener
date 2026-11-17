// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryDurationTest {
    private val minute = 60_000L
    private val hour = 60 * minute

    @Test
    fun horasSaoArredondadasParaOMaisProximo() {
        assertEquals(32, libraryTotalTime(32 * hour + 20 * minute).hours)
        assertEquals(33, libraryTotalTime(32 * hour + 30 * minute).hours)
        assertEquals(1, libraryTotalTime(hour).hours)
        assertTrue(libraryTotalTime(hour).inHours)
    }

    @Test
    fun menosDeUmaHoraMostraMinutos() {
        val d = libraryTotalTime(40 * minute)
        assertFalse(d.inHours)
        assertEquals(40, d.minutes)
        // 59 min 59 s ainda é "minutos"; 1 hora exata já é "1 hora".
        assertEquals(59, libraryTotalTime(59 * minute + 59_000L).minutes)
        assertEquals(1, libraryTotalTime(hour).hours)
    }

    @Test
    fun bibliotecaVaziaOuNegativaViraZeroMinutos() {
        assertEquals(LibraryTotalTime(0, 0), libraryTotalTime(0L))
        assertEquals(LibraryTotalTime(0, 0), libraryTotalTime(-5L))
    }
}
