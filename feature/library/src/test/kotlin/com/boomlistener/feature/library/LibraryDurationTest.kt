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
        assertEquals(32, totalDuration(32 * hour + 20 * minute).hours)
        assertEquals(33, totalDuration(32 * hour + 30 * minute).hours)
        assertEquals(1, totalDuration(hour).hours)
        assertTrue(totalDuration(hour).inHours)
    }

    @Test
    fun menosDeUmaHoraMostraMinutos() {
        val d = totalDuration(40 * minute)
        assertFalse(d.inHours)
        assertEquals(40, d.minutes)
        // 29 min 59 s ainda é "minutos"; 30 min já arredonda para 1 hora.
        assertEquals(29, totalDuration(29 * minute + 59_000L).minutes)
        assertEquals(1, totalDuration(30 * minute).hours)
    }

    @Test
    fun bibliotecaVaziaOuNegativaViraZeroMinutos() {
        assertEquals(TotalDuration(0, 0), totalDuration(0L))
        assertEquals(TotalDuration(0, 0), totalDuration(-5L))
    }
}
