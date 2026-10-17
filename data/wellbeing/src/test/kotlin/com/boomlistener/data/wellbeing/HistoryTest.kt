// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.wellbeing

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

class HistoryTest {
    private val zone = ZoneId.of("America/Belem")
    // Sábado, 17/10/2026.
    private val today = LocalDate.of(2026, 10, 17)

    private fun at(day: Int, month: Int = 10, hour: Int = 12) =
        HistoryEntry("content://$month-$day-$hour", "Música", null, null,
            LocalDateTime.of(2026, month, day, hour, 0).atZone(zone).toInstant().toEpochMilli())

    @Test
    fun agrupaPorPeriodoComSemanaComecandoNoDomingo() {
        val sections = groupHistory(
            listOf(at(17), at(16), at(12), at(11), at(10), at(3), at(20, month = 9)),
            today, zone, DayOfWeek.SUNDAY,
        )
        assertEquals(
            listOf(HistoryPeriod.Today, HistoryPeriod.Yesterday, HistoryPeriod.ThisWeek, HistoryPeriod.LastWeek, HistoryPeriod.Earlier, HistoryPeriod.Earlier),
            sections.map { it.period },
        )
        // Esta semana: domingo 11 e segunda 12; semana passada: 4 a 10 (aqui, o dia 10).
        assertEquals(2, sections[2].entries.size)
        assertEquals(1, sections[3].entries.size)
        assertEquals(YearMonth.of(2026, 10), sections[4].month) // dia 3
        assertEquals(YearMonth.of(2026, 9), sections[5].month)
    }

    @Test
    fun maisRecentePrimeiroDentroDoGrupo() {
        val sections = groupHistory(listOf(at(17, hour = 9), at(17, hour = 20)), today, zone, DayOfWeek.SUNDAY)
        assertEquals(listOf(20, 9), sections.single().entries.map { it.uri.substringAfterLast('-').toInt() })
    }

    @Test
    fun cortaOQueTemMaisDeUmAno() {
        val old = HistoryEntry("velha", "x", null, null, 0)
        val new = HistoryEntry("nova", "x", null, null, 400L * 24 * 60 * 60 * 1000)
        assertEquals(listOf("nova"), addToHistory(listOf(old), new).map { it.uri })
    }
}
