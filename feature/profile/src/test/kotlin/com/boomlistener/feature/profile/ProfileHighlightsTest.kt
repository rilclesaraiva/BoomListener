// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.profile

import com.boomlistener.data.wellbeing.DailySummary
import com.boomlistener.data.wellbeing.DaySong
import com.boomlistener.data.wellbeing.DayRow
import com.boomlistener.data.wellbeing.MonthData
import com.boomlistener.data.wellbeing.StatsPeriod
import com.boomlistener.data.wellbeing.computeStatsReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ProfileHighlightsTest {
    private val today = LocalDate.of(2026, 11, 18)
    private val earth = DaySong("content://1", "September", "Earth, Wind & Fire", "The Best", listOf("Soul"))
    private val acai = DaySong("content://2", "Açaí", "Djavan", "Luz", listOf("MPB"))

    @Test
    fun destaquesNaOrdemArtistaMusicaGeneroDia() {
        val nov = MonthData(
            YearMonth.of(2026, 11),
            listOf(earth, acai),
            mapOf(3 to listOf(DayRow(0, 3, 9 * 60_000L, 0)), 4 to listOf(DayRow(1, 1, 0L, 0))),
        )
        val report = computeStatsReport(DailySummary(listOf(nov), emptyList(), 0), StatsPeriod.ThisMonth, today) { 0L }

        val highlights = highlightsOf(report)

        // "Earth, Wind & Fire" continua um artista só, apesar das vírgulas.
        assertEquals(Highlight.TopArtist("Earth, Wind & Fire", 3), highlights[0])
        assertEquals(Highlight.TopSong("content://1", "September", "Earth, Wind & Fire", 3), highlights[1])
        assertEquals(Highlight.TopGenre("Soul", 75), highlights[2])
        assertEquals(Highlight.BestDay(LocalDate.of(2026, 11, 3), 9), highlights[3])
    }

    @Test
    fun semEscutaNaoHaDestaques() {
        val report = computeStatsReport(DailySummary(emptyList(), emptyList(), 0), StatsPeriod.ThisMonth, today) { 0L }
        assertTrue(highlightsOf(report).isEmpty())
    }
}
