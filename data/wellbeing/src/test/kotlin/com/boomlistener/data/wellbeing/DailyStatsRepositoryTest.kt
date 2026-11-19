// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.wellbeing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

class DailyStatsRepositoryTest {
    private val zone = ZoneId.of("America/Belem")
    private lateinit var dir: File

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("daily").toFile()
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    private fun ms(year: Int, month: Int, day: Int) =
        LocalDateTime.of(year, month, day, 12, 0).atZone(zone).toInstant().toEpochMilli()

    private class FakeHistory(entries: List<HistoryEntry>) : ListeningHistoryRepository {
        override val history: Flow<List<HistoryEntry>> = MutableStateFlow(entries)
        override suspend fun record(entry: HistoryEntry) = Unit
        override suspend fun clear() = Unit
    }

    private class FakeStats(private val songs: List<SongStat>) : ListeningStatsRepository {
        override fun topSongs(limit: Int): Flow<List<SongStat>> = flowOf(songs)
        override suspend fun recordPlay(uri: String, title: String, artist: String?, album: String?, timestampMs: Long) = Unit
    }

    private fun repo(history: List<HistoryEntry> = emptyList(), now: Long = ms(2026, 11, 20)) =
        FileDailyStatsRepository(dir, FakeHistory(history), FakeStats(emptyList()), { zone }, { now })

    @Test
    fun primeiraLeituraCriaOMetaESeguePronta() = runBlocking {
        val summary = repo().summary.first()
        assertTrue(summary.months.isEmpty())
        assertTrue(File(dir, "meta.json").exists())
    }

    @Test
    fun mesQueNaoAbreFicaGuardadoEmVezDeSerPerdido() = runBlocking {
        val broken = File(dir, "2026-10.json").apply { writeText("{ isto não é json") }
        val summary = repo().summary.first()
        assertTrue(summary.months.isEmpty())
        // O arquivo original foi posto de lado, então a próxima gravação de outubro não o sobrescreve.
        assertFalse(broken.exists())
        assertEquals("{ isto não é json", File(dir, "2026-10.json.corrupt").readText())
    }

    @Test
    fun migracaoTrazOHistoricoAntigoUmaVezSo() = runBlocking {
        val old = HistoryEntry("content://1", "Açaí", "Djavan", "Luz", ms(2026, 9, 10))
        val repo = repo(history = listOf(old), now = ms(2026, 11, 20))
        assertFalse(repo.isMigrated())
        repo.migrateIfNeeded { listOf("MPB") }
        assertTrue(repo.isMigrated())
        val september = repo.summary.first().months.single { it.month == YearMonth.of(2026, 9) }
        assertEquals("Açaí", september.songs.single().title)
        assertEquals(listOf("MPB"), september.songs.single().genres)
        assertTrue(File(dir, "2026-09.json").exists())
        // Segunda chamada não faz nada (não duplica).
        repo.migrateIfNeeded { listOf("Rock") }
        assertEquals(listOf("MPB"), repo.summary.first().months.single { it.month == YearMonth.of(2026, 9) }.songs.single().genres)
    }

    @Test
    fun migracaoDeOutroAparelhoLidoDeNovoNaoPerdeOMes() = runBlocking {
        val old = HistoryEntry("content://1", "Açaí", "Djavan", "Luz", ms(2026, 9, 10))
        repo(history = listOf(old)).migrateIfNeeded { emptyList() }
        // Um repositório novo (app reaberto) lê o que foi gravado.
        val reopened = repo(history = listOf(old))
        assertTrue(reopened.isMigrated())
        assertEquals(1, reopened.summary.first().months.size)
    }
}
