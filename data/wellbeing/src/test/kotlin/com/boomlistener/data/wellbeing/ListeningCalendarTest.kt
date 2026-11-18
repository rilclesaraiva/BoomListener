// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.wellbeing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class ListeningCalendarTest {
    private val today = LocalDate.of(2026, 11, 18) // quarta-feira
    private val minute = 60_000L
    private val song = DaySong("content://1", "Açaí", "Djavan", "Luz", listOf("MPB"))

    private fun summary(vararg listened: Pair<LocalDate, Long>): DailySummary {
        val months = listened.groupBy { YearMonth.from(it.first) }.map { (ym, items) ->
            var data = MonthData.empty(ym)
            items.forEach { (date, ms) -> data = data.withListen(song, date.dayOfMonth).withTime(song, date.dayOfMonth, ms) }
            data
        }
        return DailySummary(months, emptyList(), 0)
    }

    @Test
    fun colunasDeSeteDiasComSemanaAtualNoFimEFuturoVazio() {
        val calendar = computeCalendar(summary(), today, DayOfWeek.SUNDAY, { 0L })
        assertEquals(17, calendar.weeks.size)
        assertEquals(true, calendar.weeks.all { it.size == 7 })
        // Semana começando no domingo 15/11: quarta (hoje) é o 4º dia; quinta em diante é futuro.
        val last = calendar.weeks.last()
        assertEquals(LocalDate.of(2026, 11, 15), last.first()?.date)
        assertEquals(today, last[3]?.date)
        assertNull(last[4])
        assertEquals(LocalDate.of(2026, 7, 26), calendar.weeks.first().first()?.date)
    }

    @Test
    fun niveisSaoRelativosAoMaiorDiaESequenciaContaDiasSeguidos() {
        val calendar = computeCalendar(
            summary(
                LocalDate.of(2026, 11, 16) to 100 * minute, // o maior
                LocalDate.of(2026, 11, 17) to 30 * minute,
                LocalDate.of(2026, 11, 18) to 5 * minute,
                LocalDate.of(2026, 11, 10) to 60 * minute,
            ),
            today,
            DayOfWeek.SUNDAY,
            { 0L },
        )
        val days = calendar.weeks.flatten().filterNotNull().associateBy { it.date }
        assertEquals(4, days.getValue(LocalDate.of(2026, 11, 16)).level)
        assertEquals(100, days.getValue(LocalDate.of(2026, 11, 16)).minutes)
        assertEquals(3, days.getValue(LocalDate.of(2026, 11, 10)).level)
        assertEquals(2, days.getValue(LocalDate.of(2026, 11, 17)).level)
        assertEquals(1, days.getValue(today).level)
        assertEquals(0, days.getValue(LocalDate.of(2026, 11, 12)).level)
        assertEquals(3, calendar.longestStreak) // 16, 17 e 18
    }

    @Test
    fun semanaComecaNaSegundaQuandoOLocalPede() {
        val calendar = computeCalendar(summary(), today, DayOfWeek.MONDAY, { 0L })
        assertEquals(LocalDate.of(2026, 11, 16), calendar.weeks.last().first()?.date)
        assertNull(calendar.weeks.last()[3])
    }
}
