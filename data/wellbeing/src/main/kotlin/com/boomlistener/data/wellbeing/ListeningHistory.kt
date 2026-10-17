// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.wellbeing

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Uma música ouvida (30 s ou mais), com a hora. */
data class HistoryEntry(
    val uri: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val timestampMs: Long,
)

enum class HistoryPeriod { Today, Yesterday, ThisWeek, LastWeek, Earlier }

/** Um grupo do histórico. [month] só existe em [HistoryPeriod.Earlier] (um grupo por mês). */
data class HistorySection(
    val period: HistoryPeriod,
    val month: YearMonth?,
    val entries: List<HistoryEntry>,
)

/**
 * Histórico de escuta: toda música ouvida por 30 s ou mais, inclusive as do
 * aleatório (quem decide é o player). Fica só no aparelho.
 */
interface ListeningHistoryRepository {
    /** Da mais recente para a mais antiga. */
    val history: Flow<List<HistoryEntry>>

    suspend fun record(entry: HistoryEntry)

    suspend fun clear()
}

// Guarda até um ano, no máximo 5.000 reproduções (o arquivo fica pequeno).
private const val MAX_ENTRIES = 5_000
private const val MAX_AGE_MS = 365L * 24 * 60 * 60 * 1000

internal class FileListeningHistoryRepository(context: Context) : ListeningHistoryRepository {
    private val file = File(context.filesDir, "stats/history.json").apply { parentFile?.mkdirs() }
    private val mutex = Mutex()
    private val entries = MutableStateFlow<List<HistoryEntry>?>(null)

    override val history: Flow<List<HistoryEntry>> = entries
        .onStart { ensureLoaded() }
        .filterNotNull()

    private suspend fun ensureLoaded() {
        if (entries.value != null) return
        withContext(Dispatchers.IO) {
            mutex.withLock {
                if (entries.value == null) entries.value = runCatching { readHistory(file) }.getOrDefault(emptyList())
            }
        }
    }

    override suspend fun record(entry: HistoryEntry) {
        ensureLoaded()
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val updated = addToHistory(entries.value.orEmpty(), entry)
                entries.value = updated
                runCatching { writeHistory(file, updated) }
            }
        }
    }

    override suspend fun clear() {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                entries.value = emptyList()
                runCatching { file.delete() }
            }
        }
    }
}

/** Põe [entry] no topo e corta o que passou do limite de idade ou de tamanho. */
internal fun addToHistory(current: List<HistoryEntry>, entry: HistoryEntry): List<HistoryEntry> {
    val oldest = entry.timestampMs - MAX_AGE_MS
    return (listOf(entry) + current)
        .filter { it.timestampMs >= oldest }
        .sortedByDescending { it.timestampMs }
        .take(MAX_ENTRIES)
}

/**
 * Separa o histórico em Hoje, Ontem, Esta semana, Semana passada e, antes
 * disso, um grupo por mês. A semana começa em [firstDayOfWeek] (domingo no
 * Brasil, segunda em outros países).
 */
fun groupHistory(
    entries: List<HistoryEntry>,
    today: LocalDate,
    zone: ZoneId,
    firstDayOfWeek: DayOfWeek,
): List<HistorySection> {
    val yesterday = today.minusDays(1)
    val weekStart = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    val lastWeekStart = weekStart.minusWeeks(1)
    val sections = linkedMapOf<Pair<HistoryPeriod, YearMonth?>, MutableList<HistoryEntry>>()
    for (entry in entries.sortedByDescending { it.timestampMs }) {
        val date = Instant.ofEpochMilli(entry.timestampMs).atZone(zone).toLocalDate()
        val key = when {
            date == today -> HistoryPeriod.Today to null
            date == yesterday -> HistoryPeriod.Yesterday to null
            !date.isBefore(weekStart) && date.isBefore(today) -> HistoryPeriod.ThisWeek to null
            !date.isBefore(lastWeekStart) && date.isBefore(weekStart) -> HistoryPeriod.LastWeek to null
            date.isAfter(today) -> HistoryPeriod.Today to null // relógio adiantado: fica em hoje
            else -> HistoryPeriod.Earlier to YearMonth.from(date)
        }
        sections.getOrPut(key) { mutableListOf() } += entry
    }
    return sections.map { (key, list) -> HistorySection(key.first, key.second, list) }
}

private fun readHistory(file: File): List<HistoryEntry> {
    if (!file.exists()) return emptyList()
    val array = JSONArray(file.readText())
    return (0 until array.length()).map { index ->
        val json = array.getJSONObject(index)
        HistoryEntry(
            uri = json.getString("uri"),
            title = json.optString("title"),
            artist = json.optString("artist").takeIf { it.isNotEmpty() },
            album = json.optString("album").takeIf { it.isNotEmpty() },
            timestampMs = json.optLong("at"),
        )
    }
}

private fun writeHistory(file: File, entries: List<HistoryEntry>) {
    val array = JSONArray()
    entries.forEach { entry ->
        array.put(
            JSONObject()
                .put("uri", entry.uri)
                .put("title", entry.title)
                .put("artist", entry.artist.orEmpty())
                .put("album", entry.album.orEmpty())
                .put("at", entry.timestampMs),
        )
    }
    val temp = File(file.parentFile, file.name + ".tmp")
    temp.writeText(array.toString())
    temp.renameTo(file)
}

/** Ponto de acesso provisório enquanto o app não tem Hilt. */
object HistoryData {
    @Volatile
    private var instance: ListeningHistoryRepository? = null

    fun repository(context: Context): ListeningHistoryRepository =
        instance ?: synchronized(this) {
            instance ?: FileListeningHistoryRepository(context.applicationContext).also { instance = it }
        }
}
