// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.wellbeing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

class DailySummaryTest {
    private val zone = ZoneId.of("America/Belem")
    private val nov = YearMonth.of(2026, 11)

    private fun ms(month: Int, day: Int, hour: Int = 12, year: Int = 2026) =
        LocalDateTime.of(year, month, day, hour, 0).atZone(zone).toInstant().toEpochMilli()

    private fun entry(uri: String, title: String, artist: String?, at: Long) = HistoryEntry(uri, title, artist, null, at)

    private val djavan = DaySong("content://1", "Açaí", "Djavan", "Luz", listOf("MPB"))
    private val earth = DaySong("content://2", "September", "Earth, Wind & Fire", null, listOf("Soul", "Funk"))

    @Test
    fun separaGenerosDaTag() {
        assertEquals(listOf("Rock", "Pop"), splitGenres(" Rock; Pop ;; rock"))
        assertEquals(emptyList<String>(), splitGenres(null))
        assertEquals(emptyList<String>(), splitGenres("  "))
    }

    @Test
    fun somaReproducoesNoMesmoDia() {
        val month = MonthData.empty(nov).withListen(djavan, 17).withListen(djavan, 17).withListen(earth, 17).withListen(djavan, 18)
        assertEquals(2, month.songs.size)
        assertEquals(listOf(DayRow(0, 2, 0, 0), DayRow(1, 1, 0, 0)), month.days[17])
        assertEquals(listOf(DayRow(0, 1, 0, 0)), month.days[18])
    }

    @Test
    fun tagNovaSubstituiAAntigaMasGeneroVazioNaoApagaOQueSeSabia() {
        val renamed = djavan.copy(title = "Açaí (ao vivo)", genres = emptyList())
        val month = MonthData.empty(nov).withListen(djavan, 1).withListen(renamed, 2)
        assertEquals("Açaí (ao vivo)", month.songs.single().title)
        assertEquals(listOf("MPB"), month.songs.single().genres)
    }

    @Test
    fun tempoSomaNoDiaSemContarReproducao() {
        val month = MonthData.empty(nov).withListen(djavan, 17)
        val timed = month.withTime(djavan, 17, 90_000).withTime(djavan, 17, 30_000)
        assertEquals(DayRow(0, 1, 120_000, 0), timed.days[17]!!.single())
        assertEquals(month, month.withTime(djavan, 17, 0))
    }

    @Test
    fun musicaPuladaTemTempoMasNenhumaReproducao() {
        val month = MonthData.empty(nov).withTime(earth, 5, 12_000)
        assertEquals(listOf(earth), month.songs)
        assertEquals(DayRow(0, 0, 12_000, 0), month.days[5]!!.single())
    }

    @Test
    fun tempoEmOutroDiaCriaLinhaSemReproducao() {
        val month = MonthData.empty(nov).withListen(djavan, 17).withTime(djavan, 18, 5_000)
        assertEquals(DayRow(0, 0, 5_000, 0), month.days[18]!!.single())
    }

    @Test
    fun juntaMesesSomandoLinhasEUnindoMusicas() {
        val live = MonthData.empty(nov).withListen(djavan, 17).withTime(djavan, 17, 60_000)
        val old = MonthData.empty(nov).withListen(earth, 3, estimated = true).withListen(djavan, 17, estimated = true)
        val merged = live.merge(old)
        assertEquals(listOf("content://1", "content://2"), merged.songs.map { it.uri })
        assertEquals(DayRow(0, 2, 60_000, 1), merged.days[17]!!.single())
        assertEquals(DayRow(1, 1, 0, 1), merged.days[3]!!.single())
    }

    @Test
    fun migraAHistoriaAntesDoCorteComAcentosEFeat() {
        val history = listOf(
            entry("content://1", "Açaí", "Djavan", ms(10, 30)),
            entry("content://2", "September", "Earth, Wind & Fire", ms(11, 2)),
            entry("content://3", "Música (feat. Fulano)", "Beltrano feat. Fulano", ms(11, 2, hour = 20)),
            entry("content://1", "Açaí", "Djavan", ms(11, 20)), // depois do corte: já foi gravada ao vivo
        )
        val months = monthsFromHistory(history, ms(11, 10), zone) { if (it == "content://1") listOf("MPB") else emptyList() }
        assertEquals(listOf(YearMonth.of(2026, 10), nov), months.map { it.month })
        val october = months[0]
        assertEquals(listOf("MPB"), october.songs.single().genres)
        assertEquals(DayRow(0, 1, 0, 1), october.days[30]!!.single())
        val november = months[1]
        assertEquals(2, november.days[2]!!.size)
        assertEquals("Earth, Wind & Fire", november.songs[0].artist) // vírgula no nome continua um artista só
        assertNull(november.days[20])
    }

    @Test
    fun migracaoUsaOFusoDoAparelhoParaOMesDaMeiaNoite() {
        // 31/10 às 23:30 em Belém já é 01/11 em UTC: o dia certo é o local.
        val late = LocalDateTime.of(2026, 10, 31, 23, 30).atZone(zone).toInstant().toEpochMilli()
        val months = monthsFromHistory(listOf(entry("content://1", "A", null, late)), Long.MAX_VALUE, zone) { emptyList() }
        assertEquals(YearMonth.of(2026, 10), months.single().month)
        assertNotNull(months.single().days[31])
    }

    @Test
    fun legadoSoGuardaOQueAsMaisOuvidasTemAMaisQueOHistorico() {
        val plays = listOf(
            SongStat("content://1", "Açaí", "Djavan", null, plays = 10, lastPlayedMs = 5),
            SongStat("content://2", "September", null, null, plays = 2, lastPlayedMs = 6),
            SongStat("content://3", "Só histórico", null, null, plays = 1, lastPlayedMs = 7),
        )
        val history = listOf(
            entry("content://1", "Açaí", "Djavan", 1),
            entry("content://1", "Açaí", "Djavan", 2),
            entry("content://2", "September", null, 3),
            entry("content://2", "September", null, 4),
            entry("content://2", "September", null, 5), // o histórico tem mais (aleatório)
        )
        val legacy = legacyFromPlays(plays, history) { listOf("MPB") }
        assertEquals(listOf("content://1", "content://3"), legacy.map { it.uri })
        assertEquals(8, legacy[0].plays)
        assertEquals(1, legacy[1].plays)
        assertEquals(listOf("MPB"), legacy[0].genres)
    }

    @Test
    fun jsonDoMesVoltaIgual() {
        val month = MonthData.empty(nov).withListen(djavan, 17).withListen(earth.copy(artist = null, album = null), 17)
            .withTime(djavan, 17, 61_000).withListen(djavan, 30, estimated = true)
        assertEquals(month, decodeMonth(nov, encodeMonth(month)))
    }

    @Test
    fun arquivoQuebradoOuDeOutroFormatoNaoDerrubaNada() {
        assertNull(decodeMonth(nov, ""))
        assertNull(decodeMonth(nov, "{\"v\":2,\"songs\":[],\"days\":{}}"))
        assertNull(decodeMonth(nov, "não é json"))
        assertTrue(decodeLegacy("lixo").isEmpty())
        assertNull(decodeMeta("lixo"))
    }

    @Test
    fun linhaDeMusicaInexistenteEhDescartadaNaLeitura() {
        val text = "{\"v\":1,\"songs\":[{\"u\":\"content://1\",\"t\":\"A\",\"a\":\"\",\"b\":\"\",\"g\":[]}],\"days\":{\"5\":[[0,1,0,0],[7,1,0,0]]}}"
        val month = decodeMonth(nov, text)!!
        assertEquals(listOf(DayRow(0, 1, 0, 0)), month.days[5])
    }

    @Test
    fun legadoEMetaVoltamIguais() {
        val legacy = listOf(LegacySong("content://1", "Açaí", "Djavan", null, listOf("MPB", "Pop"), 8, 99))
        assertEquals(legacy, decodeLegacy(encodeLegacy(legacy)))
        val meta = DailyMeta(123, true)
        assertEquals(meta, decodeMeta(encodeMeta(meta)))
    }
}
