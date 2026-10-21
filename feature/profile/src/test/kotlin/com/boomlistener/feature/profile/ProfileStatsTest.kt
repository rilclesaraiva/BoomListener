// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.profile

import com.boomlistener.data.wellbeing.HistoryEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class ProfileStatsTest {
    private val zone = ZoneId.of("America/Belem")
    private val today = LocalDate.of(2026, 10, 17)

    private fun play(uri: String, artist: String?, month: Int, day: Int, hour: Int) =
        HistoryEntry(uri, uri, artist, null, LocalDateTime.of(2026, month, day, hour, 0).atZone(zone).toInstant().toEpochMilli())

    @Test
    fun numerosDoMesSemanaPartesDoDiaEArtistas() {
        val history = listOf(
            play("a", "Djavan", 10, 17, 9),
            play("a", "djavan", 10, 17, 21),
            play("b", "Chikoi The Maid", 10, 15, 23),
            play("b", "Chikoi The Maid", 10, 1, 14),
            play("c", "Djavan", 9, 30, 3), // mês passado
        )
        val stats = computeProfileStats(history, mapOf("a" to 240_000, "b" to 120_000), today, zone)

        assertEquals(4, stats.monthPlays)
        assertEquals(4 + 4 + 2 + 2, stats.monthMinutes)
        assertEquals(2, stats.monthArtists) // "Djavan" e "djavan" são o mesmo
        assertEquals(LocalDate.of(2026, 10, 11), stats.week.first().date)
        assertEquals(8, stats.week.last().minutes) // hoje
        assertEquals(2, stats.week[4].minutes) // dia 15
        assertEquals(1f, stats.dayParts[DayPart.Night]) // 21h e 23h
        assertEquals(0.5f, stats.dayParts[DayPart.Morning])
        assertEquals(ArtistCount("Djavan", 3), stats.topArtists.first())
    }

    @Test
    fun semHistorico() {
        val stats = computeProfileStats(emptyList(), emptyMap(), today, zone)
        assertEquals(0, stats.monthPlays)
        assertEquals(7, stats.week.size)
        assertEquals(0f, stats.dayParts[DayPart.Night])
    }
}
