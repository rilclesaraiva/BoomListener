// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.wellbeing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class StatsReportTest {
    private val today = LocalDate.of(2026, 11, 17)
    private val minute = 60_000L

    private val djavan = DaySong("content://1", "Açaí", "Djavan", "Luz", listOf("MPB"))
    private val djavanLower = DaySong("content://2", "Oceano", "djavan", "Luz", listOf("MPB"))
    private val earth = DaySong("content://3", "September", "Earth, Wind & Fire", "The Best", listOf("Soul", "Funk"))
    private val kcat = DaySong("content://4", "Faixa", "Delinquent, KCAT", null, emptyList())

    /** Mês a partir de linhas (dia, música, reproduções, ms ouvidos, estimadas). */
    private fun month(ym: YearMonth, vararg rows: Triple<Int, DaySong, Triple<Int, Long, Int>>): MonthData {
        var data = MonthData.empty(ym)
        for ((day, song, numbers) in rows) {
            val (plays, ms, est) = numbers
            repeat(plays) { data = data.withListen(song, day) }
            if (ms > 0) data = data.withTime(song, day, ms)
            if (est > 0) {
                val id = data.songs.indexOfFirst { it.uri == song.uri }
                val list = data.days[day].orEmpty().map { if (it.song == id) it.copy(estimatedPlays = est) else it }
                data = data.copy(days = data.days + (day to list))
            }
        }
        return data
    }

    private fun row(day: Int, song: DaySong, plays: Int, ms: Long = 0, est: Int = 0) = Triple(day, song, Triple(plays, ms, est))

    private fun summary(vararg months: MonthData, legacy: List<LegacySong> = emptyList()) =
        DailySummary(months.toList(), legacy, 0)

    private val duration: (String) -> Long = { 4 * minute }

    @Test
    fun periodosEOsAnterioresEquivalentes() {
        assertEquals(LocalDate.of(2026, 10, 21) to today, periodRange(StatsPeriod.Last4Weeks, today))
        assertEquals(LocalDate.of(2026, 11, 1) to today, periodRange(StatsPeriod.ThisMonth, today))
        assertEquals(LocalDate.of(2026, 1, 1) to today, periodRange(StatsPeriod.ThisYear, today))
        assertEquals(null to today, periodRange(StatsPeriod.AllTime, today))
        assertEquals(LocalDate.of(2026, 9, 23) to LocalDate.of(2026, 10, 20), previousRange(StatsPeriod.Last4Weeks, today))
        assertEquals(LocalDate.of(2026, 10, 1) to LocalDate.of(2026, 10, 31), previousRange(StatsPeriod.ThisMonth, today))
        assertEquals(LocalDate.of(2025, 1, 1) to LocalDate.of(2025, 12, 31), previousRange(StatsPeriod.ThisYear, today))
        assertNull(previousRange(StatsPeriod.AllTime, today))
    }

    @Test
    fun artistaJuntaMaiusculasMasNaoSeparaVirgulaDoNome() {
        val nov = month(YearMonth.of(2026, 11), row(3, djavan, 3), row(4, djavanLower, 2), row(5, earth, 4), row(6, kcat, 1))
        val report = computeStatsReport(summary(nov), StatsPeriod.ThisMonth, today, duration)
        assertEquals(listOf("Djavan", "Earth, Wind & Fire", "Delinquent, KCAT"), report.artists.map { it.name })
        assertEquals(5, report.artists[0].plays)
        assertEquals(3, report.artistCount)
        assertEquals(10, report.plays)
    }

    @Test
    fun minutosSaoOsMedidosMaisOsEstimadosPelaDuracao() {
        // 1 reprodução com 2 min medidos + 1 reprodução antiga estimada (4 min pela duração).
        val nov = month(YearMonth.of(2026, 11), row(3, djavan, 1, ms = 2 * minute), row(3, earth, 1, est = 1))
        val report = computeStatsReport(summary(nov), StatsPeriod.ThisMonth, today, duration)
        assertEquals(2 + 4, report.minutes)
        assertTrue(report.hasEstimated)
        val semEstimativa = computeStatsReport(summary(month(YearMonth.of(2026, 11), row(3, djavan, 1, ms = minute))), StatsPeriod.ThisMonth, today, duration)
        assertFalse(semEstimativa.hasEstimated)
    }

    @Test
    fun musicaForaDoAparelhoSemDuracaoContaTresMinutos() {
        val nov = month(YearMonth.of(2026, 11), row(3, djavan, 1, est = 1))
        val report = computeStatsReport(summary(nov), StatsPeriod.ThisMonth, today) { 0L }
        assertEquals(3, report.minutes)
    }

    @Test
    fun periodoCortaPelasDatas() {
        val oct = month(YearMonth.of(2026, 10), row(25, djavan, 2), row(10, earth, 9))
        val nov = month(YearMonth.of(2026, 11), row(2, djavan, 1))
        val s = summary(oct, nov)
        assertEquals(1, computeStatsReport(s, StatsPeriod.ThisMonth, today, duration).plays)
        assertEquals(3, computeStatsReport(s, StatsPeriod.Last4Weeks, today, duration).plays) // 21/10 em diante
        assertEquals(12, computeStatsReport(s, StatsPeriod.ThisYear, today, duration).plays)
    }

    @Test
    fun legadoSoEntraEmSempreECombinaComOsRankings() {
        val nov = month(YearMonth.of(2026, 11), row(3, djavan, 1))
        val legacy = listOf(LegacySong("content://1", "Açaí", "Djavan", "Luz", listOf("MPB"), 10, 0))
        val s = summary(nov, legacy = legacy)
        assertEquals(1, computeStatsReport(s, StatsPeriod.ThisYear, today, duration).plays)
        val all = computeStatsReport(s, StatsPeriod.AllTime, today, duration)
        assertEquals(11, all.plays)
        assertEquals(11, all.songs.single().plays)
        assertEquals(11, all.artists.single().plays)
        assertEquals(1, all.activeDays) // sem data: não conta dia
        assertTrue(all.hasEstimated)
    }

    @Test
    fun setasComparamComOMesAnterior() {
        val oct = month(YearMonth.of(2026, 10), row(5, djavan, 9), row(6, earth, 5), row(7, kcat, 1))
        val nov = month(YearMonth.of(2026, 11), row(5, earth, 9), row(6, djavan, 5), row(7, DaySong("content://9", "Nova", "Novo", null, emptyList()), 2))
        val report = computeStatsReport(summary(oct, nov), StatsPeriod.ThisMonth, today, duration)
        assertEquals(listOf("Earth, Wind & Fire", "Djavan", "Novo"), report.artists.map { it.name })
        assertEquals(RankChange.Up(1), report.artists[0].change)
        assertEquals(RankChange.Down(1), report.artists[1].change)
        assertEquals(RankChange.New, report.artists[2].change)
    }

    @Test
    fun semPeriodoAnteriorNaoHaSeta() {
        val nov = month(YearMonth.of(2026, 11), row(3, djavan, 1))
        assertEquals(RankChange.None, computeStatsReport(summary(nov), StatsPeriod.ThisMonth, today, duration).artists.single().change)
        assertEquals(RankChange.None, computeStatsReport(summary(nov), StatsPeriod.AllTime, today, duration).artists.single().change)
    }

    @Test
    fun diaRecordeSequenciaEDiaComMaisArtistas() {
        val nov = month(
            YearMonth.of(2026, 11),
            row(1, djavan, 1, ms = 10 * minute), row(2, djavan, 1, ms = 30 * minute), row(2, earth, 1, ms = 40 * minute),
            row(3, kcat, 1, ms = minute), row(10, djavan, 1, ms = 5 * minute),
        )
        val report = computeStatsReport(summary(nov), StatsPeriod.ThisMonth, today, duration)
        assertEquals(RecordDay(LocalDate.of(2026, 11, 2), 70), report.bestDay)
        assertEquals(3, report.longestStreak)
        assertEquals(MostArtistsDay(LocalDate.of(2026, 11, 2), 2), report.mostArtistsDay)
        assertEquals(4, report.activeDays)
    }

    @Test
    fun sequenciaContaDiasSeguidosAtravesDoMes() {
        val days = listOf(LocalDate.of(2026, 10, 30), LocalDate.of(2026, 10, 31), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 5))
        assertEquals(3, longestStreak(days))
        assertEquals(0, longestStreak(emptyList()))
    }

    @Test
    fun generosSomamUmaVezPorGeneroEDaoAParteDeCadaUm() {
        val nov = month(YearMonth.of(2026, 11), row(3, djavan, 2), row(4, earth, 2), row(5, kcat, 3))
        val report = computeStatsReport(summary(nov), StatsPeriod.ThisMonth, today, duration)
        assertEquals(3, report.genreCount)
        assertEquals(setOf("MPB", "Soul", "Funk"), report.genres.map { it.genre }.toSet())
        assertEquals(1f, report.genres.sumOf { it.share.toDouble() }.toFloat(), 0.001f)
        assertEquals(1f / 3, report.genres.first { it.genre == "MPB" }.share, 0.001f)
    }

    @Test
    fun albumJuntaPorAlbumEArtistaSemMisturarHomonimos() {
        val other = DaySong("content://7", "Outra", "Outro", "Luz", emptyList())
        val nov = month(YearMonth.of(2026, 11), row(3, djavan, 2), row(4, djavanLower, 1), row(5, other, 5))
        val report = computeStatsReport(summary(nov), StatsPeriod.ThisMonth, today, duration)
        assertEquals(2, report.albums.size)
        assertEquals("Outro", report.albums[0].artist)
        assertEquals(3, report.albums.first { it.artist == "Djavan" }.plays)
    }

    @Test
    fun top100NuncaPassaDoLimiteERespeitaEmpatePeloNome() {
        val songs = (1..130).map { DaySong("content://s$it", "Faixa $it", "Artista $it", null, emptyList()) }
        val nov = month(YearMonth.of(2026, 11), *songs.map { row(2, it, 1) }.toTypedArray())
        val report = computeStatsReport(summary(nov), StatsPeriod.ThisMonth, today, duration)
        assertEquals(TOP_LIMIT, report.songs.size)
        assertEquals(TOP_LIMIT, report.artists.size)
        assertEquals("Faixa 1", report.songs.first().title)
    }

    @Test
    fun sprintPrecisaDeDoisMesesEMostraAPosicaoDeCadaUm() {
        val oct = month(YearMonth.of(2026, 10), row(5, djavan, 9), row(6, earth, 5))
        val nov = month(YearMonth.of(2026, 11), row(5, earth, 9))
        val ano = computeStatsReport(summary(oct, nov), StatsPeriod.ThisYear, today, duration)
        val sprint = ano.sprint!!
        assertEquals(listOf(YearMonth.of(2026, 10), YearMonth.of(2026, 11)), sprint.months)
        val earthRow = sprint.rows.first { it.name == "Earth, Wind & Fire" }
        assertEquals(listOf(2, 1), earthRow.ranks)
        assertEquals(listOf(1, null), sprint.rows.first { it.name == "Djavan" }.ranks)
        assertNull(computeStatsReport(summary(nov), StatsPeriod.ThisYear, today, duration).sprint)
    }

    @Test
    fun semNadaNoPeriodoVoltaVazioSemQuebrar() {
        val report = computeStatsReport(summary(), StatsPeriod.AllTime, today, duration)
        assertEquals(0, report.plays)
        assertNull(report.bestDay)
        assertNull(report.sprint)
        assertTrue(report.artists.isEmpty())
    }

    @Test
    fun descobertasSaoArtistasQueSoAparecemNoPeriodo() {
        val oct = month(YearMonth.of(2026, 10), row(5, djavan, 4))
        val nov = month(YearMonth.of(2026, 11), row(3, djavan, 2), row(4, earth, 6), row(5, kcat, 1))
        val report = computeStatsReport(summary(oct, nov), StatsPeriod.ThisMonth, today, duration)
        assertEquals(listOf("Earth, Wind & Fire", "Delinquent, KCAT"), report.discoveries)
        assertEquals(2, report.discoveryCount)
        assertEquals(LocalDate.of(2026, 10, 5), report.dataSince)
        // Sem nada antes do período, tudo seria "novo": não há descobertas a mostrar.
        assertTrue(computeStatsReport(summary(nov), StatsPeriod.ThisMonth, today, duration).discoveries.isEmpty())
        assertTrue(computeStatsReport(summary(oct, nov), StatsPeriod.AllTime, today, duration).discoveries.isEmpty())
    }

    @Test
    fun horarioFavoritoVemDoTempoPorHoraEIgnoraMesesSemMedicao() {
        val comHoras = month(YearMonth.of(2026, 11), row(3, djavan, 1)).withHourTime(22, 5 * minute).withHourTime(8, 2 * minute).withHourTime(22, minute)
        val report = computeStatsReport(summary(comHoras), StatsPeriod.ThisYear, today, duration)
        assertEquals(22, report.favoriteHour)
        assertEquals(24, report.hourLevels.size)
        assertEquals(1f, report.hourLevels[22], 0.0001f)
        assertEquals(2f / 6f, report.hourLevels[8], 0.0001f)
        assertEquals(0f, report.hourLevels[3], 0.0001f)
        val semHoras = month(YearMonth.of(2026, 11), row(3, djavan, 1))
        val semMedicao = computeStatsReport(summary(semHoras), StatsPeriod.ThisYear, today, duration)
        assertNull(semMedicao.favoriteHour)
        assertTrue(semMedicao.hourLevels.isEmpty())
    }

    @Test
    fun wrappedAbreComUmMesDeDados() {
        assertEquals(WRAPPED_MIN_DAYS, wrappedDaysLeft(null, today))
        assertEquals(13, wrappedDaysLeft(LocalDate.of(2026, 10, 31), today))
        assertEquals(0, wrappedDaysLeft(LocalDate.of(2026, 10, 18), today))
        assertEquals(0, wrappedDaysLeft(LocalDate.of(2025, 1, 1), today))
    }

    @Test
    fun musicaDoWrappedPulaAsApagadas() {
        val songs = listOf(SongRank("a", "A", null, 9, 1), SongRank("b", "B", null, 5, 1), SongRank("c", "C", null, 2, 1))
        assertEquals("a", wrappedSong(songs) { true }?.uri)
        assertEquals("b", wrappedSong(songs) { it != "a" }?.uri)
        assertEquals("c", wrappedSong(songs) { it == "c" }?.uri)
        assertNull(wrappedSong(songs) { false })
    }

    @Test
    fun avisoDoWrappedSoEmDezembroComDadosELigado() {
        val since = LocalDate.of(2026, 3, 1)
        val dec = LocalDate.of(2026, 12, 1)
        assertTrue(wrappedAlertDue(dec, true, 0, since))
        assertFalse(wrappedAlertDue(dec, false, 0, since)) // opção desligada
        assertFalse(wrappedAlertDue(dec, true, 2026, since)) // já avisou neste ano
        assertTrue(wrappedAlertDue(dec.plusYears(1), true, 2026, since)) // outro ano
        assertFalse(wrappedAlertDue(LocalDate.of(2026, 11, 30), true, 0, since)) // ainda não é dezembro
        assertFalse(wrappedAlertDue(LocalDate.of(2027, 1, 2), true, 0, since)) // passou: não manda o do ano antigo
        assertFalse(wrappedAlertDue(dec, true, 0, null)) // sem escuta
        assertFalse(wrappedAlertDue(dec, true, 0, LocalDate.of(2026, 11, 20))) // menos de 30 dias
    }

    @Test
    fun cartaoDoPerfilSomeQuandoDispensado() {
        val since = LocalDate.of(2026, 3, 1)
        val dec = LocalDate.of(2026, 12, 5)
        assertTrue(wrappedCardVisible(dec, true, 0, 0, since))
        assertFalse(wrappedCardVisible(dec, true, 0, 2026, since))
        assertFalse(wrappedCardVisible(dec, true, 2026, 0, since)) // a notificação já saiu
        assertFalse(wrappedCardVisible(dec, false, 0, 0, since))
    }

    @Test
    fun curvaDoDiaSuavizaOsPicosEEmendaAMeiaNoite() {
        val pico = List(24) { if (it == 12) 1f else 0f }
        val suave = smoothHourLevels(pico)
        assertEquals(24, suave.size)
        assertEquals(1f, suave[12], 0.0001f)
        assertEquals(suave[11], suave[13], 0.0001f) // simétrica
        assertTrue(suave[11] > 0.5f && suave[9] > 0f && suave[8] == 0f)
        // Uma escuta às 23 h chega até a madrugada seguinte.
        val noite = smoothHourLevels(List(24) { if (it == 23) 1f else 0f })
        assertTrue(noite[0] > 0f && noite[2] > 0f)
        // Sem dados ou fora das 24 posições: devolve como veio.
        assertTrue(smoothHourLevels(emptyList()).isEmpty())
        assertEquals(List(24) { 0f }, smoothHourLevels(List(24) { 0f }))
    }
}
