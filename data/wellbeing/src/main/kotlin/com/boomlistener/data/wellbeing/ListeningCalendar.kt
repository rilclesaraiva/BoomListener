// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.wellbeing

import java.time.DayOfWeek
import java.time.LocalDate

/** Um dia do calendário de escuta: [level] de 0 (nada) a 4 (o dia que mais ouviu na janela). */
data class CalendarDay(val date: LocalDate, val minutes: Int, val level: Int)

/**
 * O calendário de escuta: [weeks] são as colunas (a mais antiga primeiro), cada uma com os
 * 7 dias da semana (null = ainda não chegou, depois de hoje). [longestStreak] é a maior
 * sequência de dias com escuta dentro da janela.
 */
data class ListeningCalendar(val weeks: List<List<CalendarDay?>>, val longestStreak: Int)

/**
 * Calendário das últimas [weekCount] semanas (a atual inclusive), a partir do resumo diário.
 * O nível de cada dia é relativo ao dia que mais ouviu na própria janela.
 */
fun computeCalendar(
    summary: DailySummary,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek,
    durationOf: (String) -> Long,
    weekCount: Int = CALENDAR_WEEKS,
): ListeningCalendar {
    val weekStart = today.minusDays(((today.dayOfWeek.value - firstDayOfWeek.value + 7) % 7).toLong())
    val from = weekStart.minusWeeks((weekCount - 1).toLong())
    val msByDay = daysIn(summary, from, today, durationOf).groupBy { it.date }.mapValues { (_, rows) -> rows.sumOf { it.ms } }
    val max = msByDay.values.maxOrNull() ?: 0L
    val weeks = (0 until weekCount).map { week ->
        (0 until 7).map { day ->
            val date = from.plusWeeks(week.toLong()).plusDays(day.toLong())
            if (date.isAfter(today)) {
                null
            } else {
                val ms = msByDay[date] ?: 0L
                val level = if (ms <= 0L || max <= 0L) 0 else ((ms * 4 + max - 1) / max).toInt().coerceIn(1, 4)
                CalendarDay(date, (ms / 60_000L).toInt(), level)
            }
        }
    }
    return ListeningCalendar(weeks, longestStreak(msByDay.filterValues { it > 0L }.keys))
}

const val CALENDAR_WEEKS = 17
