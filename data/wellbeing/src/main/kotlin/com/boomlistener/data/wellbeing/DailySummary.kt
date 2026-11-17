// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.wellbeing

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/**
 * Resumo diário da escuta (base das estatísticas e do Wrapped). Ao contrário do
 * histórico (1 ano ou 5.000 reproduções), **nunca é apagado**. Fica só no
 * aparelho, em `stats/daily/AAAA-MM.json` (um arquivo por mês) e entra no backup.
 *
 * Regra de contagem: a do histórico, ou seja, toda música ouvida por 30 s ou
 * mais, inclusive as do aleatório.
 */

/** Uma música do resumo, com o que se sabia dela quando foi ouvida. */
data class DaySong(
    val uri: String,
    val title: String,
    val artist: String?,
    val album: String?,
    /** Gêneros da tag no momento da escuta ("Rock; Pop" vira dois). */
    val genres: List<String>,
)

/**
 * O que uma música teve em um dia. [listenedMs] é o tempo ouvido de verdade
 * (medido pelo player, só depois da atualização que criou o resumo).
 * [estimatedPlays] são as reproduções trazidas do histórico antigo, que não
 * tinham o tempo medido: quem mostra minutos as estima pela duração da música.
 */
data class DayRow(val song: Int, val plays: Int, val listenedMs: Long, val estimatedPlays: Int)

/** Um mês do resumo. [days] vai do dia do mês (1 a 31) para as linhas daquele dia. */
data class MonthData(
    val month: YearMonth,
    val songs: List<DaySong>,
    val days: Map<Int, List<DayRow>>,
    /**
     * Tempo ouvido por hora do dia (24 posições, na hora local de quem ouviu) neste mês;
     * vazio se nada foi medido (meses migrados do histórico não têm). Base do "horário favorito".
     */
    val hourMs: List<Long> = emptyList(),
) {
    companion object {
        fun empty(month: YearMonth) = MonthData(month, emptyList(), emptyMap())
    }
}

/**
 * Reproduções antigas que o histórico (limitado) já não tem, mas as "mais
 * ouvidas" ainda guardam: sem data, entram só nas contas de "sempre".
 */
data class LegacySong(
    val uri: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val genres: List<String>,
    val plays: Int,
    val lastPlayedMs: Long,
)

/** Tudo o que o resumo guarda. */
data class DailySummary(
    val months: List<MonthData>,
    val legacy: List<LegacySong>,
    /** Daqui para frente o tempo é medido; antes disso os minutos são estimados. 0 = ainda não definido. */
    val realSinceMs: Long,
)

/** Gêneros de uma tag ("Rock; Pop"), sem vazios nem repetidos. */
fun splitGenres(raw: String?): List<String> =
    raw?.split(';')?.map { it.trim() }?.filter { it.isNotEmpty() }?.distinctBy { it.lowercase() }.orEmpty()

/** Acha [song] no mês (ou a põe) e devolve as músicas e a posição dela. */
private fun MonthData.withSong(song: DaySong): Pair<List<DaySong>, Int> {
    val found = songs.indexOfFirst { it.uri == song.uri }
    if (found < 0) return (songs + song) to songs.size
    // Tag que mudou vale a mais nova; gênero vazio não apaga o que já se sabia.
    val merged = song.copy(genres = song.genres.ifEmpty { songs[found].genres })
    return songs.toMutableList().also { it[found] = merged } to found
}

/** Soma uma reprodução (30 s ouvidos) de [song] no [day] do mês. */
internal fun MonthData.withListen(song: DaySong, day: Int, estimated: Boolean = false): MonthData {
    val (newSongs, id) = withSong(song)
    val rows = days[day].orEmpty()
    val at = rows.indexOfFirst { it.song == id }
    val extra = if (estimated) 1 else 0
    val newRows = if (at >= 0) {
        rows.toMutableList().also {
            val row = it[at]
            it[at] = row.copy(plays = row.plays + 1, estimatedPlays = row.estimatedPlays + extra)
        }
    } else {
        rows + DayRow(id, 1, 0L, extra)
    }
    return copy(songs = newSongs, days = days + (day to newRows))
}

/** Soma [ms] de tempo ouvido de [song] no [day] (sem contar reprodução: música pulada também tem tempo). */
internal fun MonthData.withTime(song: DaySong, day: Int, ms: Long): MonthData {
    if (ms <= 0) return this
    val (newSongs, id) = withSong(song)
    val rows = days[day].orEmpty()
    val at = rows.indexOfFirst { it.song == id }
    val newRows = if (at >= 0) {
        rows.toMutableList().also { it[at] = it[at].copy(listenedMs = it[at].listenedMs + ms) }
    } else {
        rows + DayRow(id, 0, ms, 0)
    }
    return copy(songs = newSongs, days = days + (day to newRows))
}

/** Soma [ms] de tempo ouvido na [hour] (0 a 23) do dia. */
internal fun MonthData.withHourTime(hour: Int, ms: Long): MonthData {
    if (ms <= 0 || hour !in 0..23) return this
    val hours = hourMs.ifEmpty { List(24) { 0L } }
    return copy(hourMs = hours.mapIndexed { index, value -> if (index == hour) value + ms else value })
}

/** Junta dois resumos do mesmo mês (o que já era gravado + o que veio da migração). */
internal fun MonthData.merge(other: MonthData): MonthData {
    var allSongs = songs
    val mapping = other.songs.map { song ->
        val found = allSongs.indexOfFirst { it.uri == song.uri }
        if (found >= 0) {
            found
        } else {
            allSongs = allSongs + song
            allSongs.size - 1
        }
    }
    val merged = days.toMutableMap()
    for ((day, rows) in other.days) {
        val current = merged[day].orEmpty().toMutableList()
        for (row in rows) {
            val id = mapping[row.song]
            val at = current.indexOfFirst { it.song == id }
            if (at >= 0) {
                val old = current[at]
                current[at] = DayRow(id, old.plays + row.plays, old.listenedMs + row.listenedMs, old.estimatedPlays + row.estimatedPlays)
            } else {
                current += row.copy(song = id)
            }
        }
        merged[day] = current
    }
    val hours = if (hourMs.isEmpty() && other.hourMs.isEmpty()) emptyList() else List(24) { (hourMs.getOrNull(it) ?: 0L) + (other.hourMs.getOrNull(it) ?: 0L) }
    return MonthData(month, allSongs, merged, hours)
}

/**
 * Migração: monta o resumo dos meses a partir do histórico antigo, só com o
 * que aconteceu **antes** de [beforeMs] (depois disso o resumo já recebe cada
 * reprodução ao vivo). [genresOf] dá os gêneros atuais de uma música (vazio se
 * ela saiu do aparelho).
 */
internal fun monthsFromHistory(
    history: List<HistoryEntry>,
    beforeMs: Long,
    zone: ZoneId,
    genresOf: (String) -> List<String>,
): List<MonthData> {
    val result = linkedMapOf<YearMonth, MonthData>()
    for (entry in history.filter { it.timestampMs < beforeMs }.sortedBy { it.timestampMs }) {
        val date = Instant.ofEpochMilli(entry.timestampMs).atZone(zone).toLocalDate()
        val month = YearMonth.from(date)
        val song = DaySong(entry.uri, entry.title, entry.artist, entry.album, genresOf(entry.uri))
        result[month] = (result[month] ?: MonthData.empty(month)).withListen(song, date.dayOfMonth, estimated = true)
    }
    return result.values.sortedBy { it.month }
}

/**
 * Reproduções que as "mais ouvidas" têm a mais que o histórico (que só guarda
 * 1 ano ou 5.000): ficam sem data, só para o ranking de "sempre".
 */
internal fun legacyFromPlays(
    plays: Collection<SongStat>,
    history: List<HistoryEntry>,
    genresOf: (String) -> List<String>,
): List<LegacySong> {
    val inHistory = history.groupingBy { it.uri }.eachCount()
    return plays.mapNotNull { stat ->
        val extra = stat.plays - (inHistory[stat.uri] ?: 0)
        if (extra <= 0) null else LegacySong(stat.uri, stat.title, stat.artist, stat.album, genresOf(stat.uri), extra, stat.lastPlayedMs)
    }.sortedBy { it.uri }
}

// ---- Arquivo (JSON) ----

internal fun encodeMonth(data: MonthData): String {
    val songs = JSONArray()
    data.songs.forEach { song ->
        songs.put(
            JSONObject()
                .put("u", song.uri)
                .put("t", song.title)
                .put("a", song.artist.orEmpty())
                .put("b", song.album.orEmpty())
                .put("g", JSONArray(song.genres)),
        )
    }
    val days = JSONObject()
    data.days.toSortedMap().forEach { (day, rows) ->
        val array = JSONArray()
        rows.forEach { row -> array.put(JSONArray(listOf(row.song, row.plays, row.listenedMs, row.estimatedPlays))) }
        days.put(day.toString(), array)
    }
    val json = JSONObject().put("v", 1).put("songs", songs).put("days", days)
    if (data.hourMs.isNotEmpty()) json.put("h", JSONArray(data.hourMs))
    return json.toString()
}

/** Lê um mês; arquivo quebrado ou de formato desconhecido vira null (nunca derruba o app). */
internal fun decodeMonth(month: YearMonth, text: String): MonthData? = runCatching {
    val json = JSONObject(text)
    if (json.optInt("v") != 1) return null
    val songsJson = json.getJSONArray("songs")
    val songs = (0 until songsJson.length()).map { index ->
        val item = songsJson.getJSONObject(index)
        val genres = item.optJSONArray("g")
        DaySong(
            uri = item.getString("u"),
            title = item.optString("t"),
            artist = item.optString("a").takeIf { it.isNotEmpty() },
            album = item.optString("b").takeIf { it.isNotEmpty() },
            genres = (0 until (genres?.length() ?: 0)).map { genres!!.getString(it) },
        )
    }
    val daysJson = json.getJSONObject("days")
    val days = daysJson.keys().asSequence().associate { key ->
        val array = daysJson.getJSONArray(key)
        key.toInt() to (0 until array.length()).map { index ->
            val row = array.getJSONArray(index)
            DayRow(row.getInt(0), row.getInt(1), row.getLong(2), row.getInt(3))
        }.filter { it.song in songs.indices }
    }
    val hoursJson = json.optJSONArray("h")
    val hours = if (hoursJson != null && hoursJson.length() == 24) (0 until 24).map { hoursJson.optLong(it) } else emptyList()
    MonthData(month, songs, days, hours)
}.getOrNull()

internal fun encodeLegacy(legacy: List<LegacySong>): String {
    val array = JSONArray()
    legacy.forEach { song ->
        array.put(
            JSONObject()
                .put("u", song.uri)
                .put("t", song.title)
                .put("a", song.artist.orEmpty())
                .put("b", song.album.orEmpty())
                .put("g", JSONArray(song.genres))
                .put("p", song.plays)
                .put("l", song.lastPlayedMs),
        )
    }
    return JSONObject().put("v", 1).put("songs", array).toString()
}

internal fun decodeLegacy(text: String): List<LegacySong> = runCatching {
    val array = JSONObject(text).getJSONArray("songs")
    (0 until array.length()).map { index ->
        val item = array.getJSONObject(index)
        val genres = item.optJSONArray("g")
        LegacySong(
            uri = item.getString("u"),
            title = item.optString("t"),
            artist = item.optString("a").takeIf { it.isNotEmpty() },
            album = item.optString("b").takeIf { it.isNotEmpty() },
            genres = (0 until (genres?.length() ?: 0)).map { genres!!.getString(it) },
            plays = item.optInt("p"),
            lastPlayedMs = item.optLong("l"),
        )
    }
}.getOrDefault(emptyList())

/** Estado da migração (`meta.json`). */
internal data class DailyMeta(val realSinceMs: Long, val migrated: Boolean)

internal fun encodeMeta(meta: DailyMeta): String =
    JSONObject().put("v", 1).put("realSince", meta.realSinceMs).put("migrated", meta.migrated).toString()

internal fun decodeMeta(text: String): DailyMeta? = runCatching {
    val json = JSONObject(text)
    DailyMeta(json.getLong("realSince"), json.optBoolean("migrated"))
}.getOrNull()
