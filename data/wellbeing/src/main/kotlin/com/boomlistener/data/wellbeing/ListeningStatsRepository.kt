// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.wellbeing

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Quantas vezes uma música contou como ouvida. */
data class SongStat(
    val uri: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val plays: Int,
    val lastPlayedMs: Long,
)

/**
 * Estatísticas de escuta (ficam só no aparelho). Quem decide se uma reprodução
 * conta é o player (PlayCountTracker); aqui só se guarda e se ordena.
 */
interface ListeningStatsRepository {
    /** As mais ouvidas, da mais tocada para a menos. */
    fun topSongs(limit: Int): Flow<List<SongStat>>

    suspend fun recordPlay(uri: String, title: String, artist: String?, album: String?, timestampMs: Long)
}

internal class FileListeningStatsRepository(context: Context) : ListeningStatsRepository {
    private val file = File(context.filesDir, "stats/plays.json").apply { parentFile?.mkdirs() }
    private val mutex = Mutex()
    private val stats = MutableStateFlow<Map<String, SongStat>?>(null)

    override fun topSongs(limit: Int): Flow<List<SongStat>> {
        if (stats.value == null) {
            // Carrega na primeira vez que alguém pede.
            stats.value = runCatching { readStats(file) }.getOrDefault(emptyMap())
        }
        return stats.map { rankSongs(it.orEmpty(), limit) }
    }

    override suspend fun recordPlay(uri: String, title: String, artist: String?, album: String?, timestampMs: Long) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val current = stats.value ?: runCatching { readStats(file) }.getOrDefault(emptyMap())
                val updated = applyPlay(current, uri, title, artist, album, timestampMs)
                stats.value = updated
                runCatching { writeStats(file, updated) }
            }
        }
    }
}

/** Soma uma reprodução (e atualiza título/artista, caso as tags tenham mudado). */
internal fun applyPlay(
    stats: Map<String, SongStat>,
    uri: String,
    title: String,
    artist: String?,
    album: String?,
    timestampMs: Long,
): Map<String, SongStat> {
    val previous = stats[uri]
    val next = SongStat(uri, title, artist, album, (previous?.plays ?: 0) + 1, timestampMs)
    return stats + (uri to next)
}

/** Mais tocadas primeiro; empate, a ouvida mais recentemente. */
internal fun rankSongs(stats: Map<String, SongStat>, limit: Int): List<SongStat> =
    stats.values.sortedWith(compareByDescending<SongStat> { it.plays }.thenByDescending { it.lastPlayedMs }).take(limit)

private fun readStats(file: File): Map<String, SongStat> {
    if (!file.exists()) return emptyMap()
    val array = JSONArray(file.readText())
    return (0 until array.length()).associate { index ->
        val json = array.getJSONObject(index)
        val stat = SongStat(
            uri = json.getString("uri"),
            title = json.optString("title"),
            artist = json.optString("artist").takeIf { it.isNotEmpty() },
            album = json.optString("album").takeIf { it.isNotEmpty() },
            plays = json.optInt("plays"),
            lastPlayedMs = json.optLong("last"),
        )
        stat.uri to stat
    }
}

private fun writeStats(file: File, stats: Map<String, SongStat>) {
    val array = JSONArray()
    stats.values.forEach { stat ->
        array.put(
            JSONObject()
                .put("uri", stat.uri)
                .put("title", stat.title)
                .put("artist", stat.artist.orEmpty())
                .put("album", stat.album.orEmpty())
                .put("plays", stat.plays)
                .put("last", stat.lastPlayedMs),
        )
    }
    // Grava num arquivo temporário e troca, para nunca deixar o arquivo pela metade.
    val temp = File(file.parentFile, file.name + ".tmp")
    temp.writeText(array.toString())
    temp.renameTo(file)
}

/** Ponto de acesso provisório enquanto o app não tem Hilt. */
object StatsData {
    @Volatile
    private var instance: ListeningStatsRepository? = null

    fun repository(context: Context): ListeningStatsRepository =
        instance ?: synchronized(this) {
            instance ?: FileListeningStatsRepository(context.applicationContext).also { instance = it }
        }
}
