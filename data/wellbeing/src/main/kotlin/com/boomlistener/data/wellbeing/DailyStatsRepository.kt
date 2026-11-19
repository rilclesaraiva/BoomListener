// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.wellbeing

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/**
 * Resumo diário da escuta (ver `DailySummary.kt`): quem grava é o player (cada
 * música ouvida por 30 s e o tempo realmente ouvido), quem lê são as telas de
 * estatísticas. Nada daqui sai do aparelho.
 */
interface DailyStatsRepository {
    /** O resumo todo; muda a cada gravação. */
    val summary: Flow<DailySummary>

    /** Uma música foi ouvida por 30 s ou mais (inclusive pelo aleatório). */
    suspend fun recordListen(
        uri: String,
        title: String,
        artist: String?,
        album: String?,
        genres: List<String>,
        timestampMs: Long,
    )

    /** [ms] de tempo realmente ouvido da música (ouvida 30 s ou pulada), no dia de [timestampMs]. */
    suspend fun recordTime(
        uri: String,
        title: String,
        artist: String?,
        album: String?,
        genres: List<String>,
        ms: Long,
        timestampMs: Long,
    )

    /** true quando o histórico antigo já foi trazido para o resumo. */
    suspend fun isMigrated(): Boolean

    /**
     * Traz o histórico e as "mais ouvidas" antigos para o resumo, uma única vez.
     * [genresOf] dá os gêneros atuais de uma música (vazio se ela não está mais no aparelho).
     */
    suspend fun migrateIfNeeded(genresOf: (String) -> List<String>)
}

private const val WRITE_DELAY_MS = 3_000L
private val MONTH_FILE = Regex("""(\d{4})-(\d{2})\.json""")

internal class FileDailyStatsRepository(
    private val dir: File,
    private val history: ListeningHistoryRepository,
    private val stats: ListeningStatsRepository,
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
    private val now: () -> Long = { System.currentTimeMillis() },
) : DailyStatsRepository {
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val state = MutableStateFlow<DailySummary?>(null)
    private var meta = DailyMeta(0L, false)
    private val dirty = mutableSetOf<YearMonth>()
    private var legacyDirty = false
    private var writeJob: Job? = null

    override val summary: Flow<DailySummary> = state.onStart { ensureLoaded() }.filterNotNull()

    private suspend fun ensureLoaded() {
        if (state.value != null) return
        withContext(Dispatchers.IO) {
            mutex.withLock {
                if (state.value == null) load()
            }
        }
    }

    // Chamar só com o mutex.
    private fun load() {
        dir.mkdirs()
        val metaFile = File(dir, "meta.json")
        meta = metaFile.takeIf { it.exists() }?.let { runCatching { decodeMeta(it.readText()) }.getOrNull() }
            ?: DailyMeta(now(), false).also { writeAtomic(metaFile, encodeMeta(it)) }
        val months = dir.listFiles().orEmpty().mapNotNull { file ->
            val match = MONTH_FILE.matchEntire(file.name) ?: return@mapNotNull null
            val month = YearMonth.of(match.groupValues[1].toInt(), match.groupValues[2].toInt())
            // Mês que não abre não é descartado em silêncio: o arquivo fica guardado ao lado
            // (.corrupt) em vez de ser sobrescrito pela próxima gravação daquele mês.
            // (decodeMonth devolve null para arquivo quebrado ou de formato desconhecido.)
            runCatching { decodeMonth(month, file.readText()) }.getOrNull() ?: run { keepCorrupt(file); null }
        }.sortedBy { it.month }
        val legacy = File(dir, "legacy.json").takeIf { it.exists() }?.let { decodeLegacy(it.readText()) }.orEmpty()
        state.value = DailySummary(months, legacy, meta.realSinceMs)
    }

    // Chamar só com o mutex.
    private fun keepCorrupt(file: File) {
        runCatching { file.renameTo(File(file.parentFile, file.name + ".corrupt")) }
    }

    override suspend fun recordListen(
        uri: String,
        title: String,
        artist: String?,
        album: String?,
        genres: List<String>,
        timestampMs: Long,
    ) {
        ensureLoaded()
        withContext(Dispatchers.IO) {
            mutex.withLock {
                // Ouvida antes de o resumo existir (e já no histórico): a migração a traz, não vale contar duas vezes.
                if (!meta.migrated && timestampMs < meta.realSinceMs) return@withLock
                val date = Instant.ofEpochMilli(timestampMs).atZone(zone()).toLocalDate()
                val song = DaySong(uri, title, artist, album, genres)
                update(YearMonth.from(date)) { (it ?: MonthData.empty(YearMonth.from(date))).withListen(song, date.dayOfMonth) }
            }
        }
    }

    override suspend fun recordTime(
        uri: String,
        title: String,
        artist: String?,
        album: String?,
        genres: List<String>,
        ms: Long,
        timestampMs: Long,
    ) {
        if (ms <= 0) return
        ensureLoaded()
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val moment = Instant.ofEpochMilli(timestampMs).atZone(zone())
                val date = moment.toLocalDate()
                val month = YearMonth.from(date)
                val song = DaySong(uri, title, artist, album, genres)
                update(month) { (it ?: MonthData.empty(month)).withTime(song, date.dayOfMonth, ms).withHourTime(moment.hour, ms) }
            }
        }
    }

    // Chamar só com o mutex e o resumo carregado.
    private fun update(month: YearMonth, change: (MonthData?) -> MonthData?) {
        val current = state.value ?: return
        val old = current.months.firstOrNull { it.month == month }
        val updated = change(old) ?: return
        if (updated == old) return
        val months = (current.months.filter { it.month != month } + updated).sortedBy { it.month }
        state.value = current.copy(months = months)
        dirty += month
        scheduleWrite()
    }

    private fun scheduleWrite() {
        if (writeJob?.isActive == true) return
        writeJob = scope.launch {
            delay(WRITE_DELAY_MS)
            mutex.withLock { flush() }
        }
    }

    // Chamar só com o mutex.
    private fun flush() {
        val current = state.value ?: return
        dir.mkdirs()
        dirty.toList().forEach { month ->
            val data = current.months.firstOrNull { it.month == month } ?: return@forEach
            runCatching { writeAtomic(File(dir, "$month.json"), encodeMonth(data)) }
        }
        dirty.clear()
        if (legacyDirty) {
            runCatching { writeAtomic(File(dir, "legacy.json"), encodeLegacy(current.legacy)) }
            legacyDirty = false
        }
    }

    override suspend fun isMigrated(): Boolean {
        ensureLoaded()
        return meta.migrated
    }

    override suspend fun migrateIfNeeded(genresOf: (String) -> List<String>) {
        ensureLoaded()
        if (meta.migrated) return
        val oldHistory = history.history.first()
        val oldPlays = stats.topSongs(Int.MAX_VALUE).first()
        withContext(Dispatchers.IO) {
            mutex.withLock {
                if (meta.migrated) return@withLock
                val current = state.value ?: return@withLock
                val fromHistory = monthsFromHistory(oldHistory, meta.realSinceMs, zone(), genresOf)
                var months = current.months
                for (migrated in fromHistory) {
                    val live = months.firstOrNull { it.month == migrated.month }
                    val joined = live?.merge(migrated) ?: migrated
                    months = (months.filter { it.month != migrated.month } + joined).sortedBy { it.month }
                    dirty += migrated.month
                }
                val legacy = legacyFromPlays(oldPlays, oldHistory, genresOf)
                state.value = current.copy(months = months, legacy = legacy)
                legacyDirty = true
                flush()
                meta = meta.copy(migrated = true)
                writeAtomic(File(dir, "meta.json"), encodeMeta(meta))
            }
        }
    }
}

/** Grava num arquivo temporário e troca, para nunca deixar o arquivo pela metade. */
private fun writeAtomic(file: File, text: String) {
    val temp = File(file.parentFile, file.name + ".tmp")
    temp.writeText(text)
    temp.renameTo(file)
}

/** Ponto de acesso provisório enquanto o app não tem Hilt. */
object DailyStatsData {
    @Volatile
    private var instance: DailyStatsRepository? = null

    fun repository(context: Context): DailyStatsRepository =
        instance ?: synchronized(this) {
            instance ?: FileDailyStatsRepository(
                File(context.applicationContext.filesDir, "stats/daily"),
                HistoryData.repository(context),
                StatsData.repository(context),
            ).also { instance = it }
        }
}
