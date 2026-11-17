// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.wellbeing

import java.time.LocalDate
import java.time.YearMonth

/**
 * Contas da tela de Estatísticas, em cima do resumo diário (puras e testadas).
 * Regra de contagem: toda música ouvida por 30 s; artista é o nome exato da tag
 * ("Earth, Wind & Fire" é um só; "Djavan" e "djavan" são o mesmo).
 */
enum class StatsPeriod { Last4Weeks, ThisMonth, ThisYear, AllTime }

/** Como um item subiu ou desceu em relação ao período anterior equivalente. */
sealed interface RankChange {
    /** Sem período anterior para comparar (ou ele está vazio). */
    data object None : RankChange
    data object New : RankChange
    data object Same : RankChange
    data class Up(val places: Int) : RankChange
    data class Down(val places: Int) : RankChange
}

data class ArtistRank(val name: String, val plays: Int, val minutes: Int, val change: RankChange)

data class SongRank(val uri: String, val title: String, val artist: String?, val plays: Int, val minutes: Int)

/** [coverUri] é uma música do álbum, para mostrar a capa. */
data class AlbumRank(val album: String, val artist: String?, val coverUri: String, val plays: Int, val minutes: Int)

/** [share] de 0 a 1 entre todos os gêneros do período. */
data class GenreRank(val genre: String, val plays: Int, val share: Float)

data class RecordDay(val date: LocalDate, val minutes: Int)

data class MostArtistsDay(val date: LocalDate, val artists: Int)

/** O Top 5 do período mês a mês: [ranks] tem uma posição por mês (null = não ouviu naquele mês). */
data class SprintRow(val name: String, val ranks: List<Int?>)

data class Sprint(val months: List<YearMonth>, val rows: List<SprintRow>)

data class StatsReport(
    val period: StatsPeriod,
    /** Primeiro e último dia do período (from é null em "sempre"). */
    val from: LocalDate?,
    val to: LocalDate,
    val plays: Int,
    val minutes: Int,
    val activeDays: Int,
    val artistCount: Int,
    val genreCount: Int,
    val bestDay: RecordDay?,
    val longestStreak: Int,
    val mostArtistsDay: MostArtistsDay?,
    val artists: List<ArtistRank>,
    val songs: List<SongRank>,
    val albums: List<AlbumRank>,
    val genres: List<GenreRank>,
    val sprint: Sprint?,
    /** Parte dos minutos é estimada pela duração das músicas (escuta de antes do resumo). */
    val hasEstimated: Boolean,
)

const val TOP_LIMIT = 100
private const val SPRINT_TOP = 5
private const val SPRINT_MONTHS = 6
private const val SPRINT_DEPTH = 10
private const val FALLBACK_DURATION_MS = 180_000L

/** Dia a dia do período e do anterior equivalente (null onde não existe). */
internal fun periodRange(period: StatsPeriod, today: LocalDate): Pair<LocalDate?, LocalDate> = when (period) {
    StatsPeriod.Last4Weeks -> today.minusDays(27) to today
    StatsPeriod.ThisMonth -> today.withDayOfMonth(1) to today
    StatsPeriod.ThisYear -> today.withDayOfYear(1) to today
    StatsPeriod.AllTime -> null to today
}

internal fun previousRange(period: StatsPeriod, today: LocalDate): Pair<LocalDate, LocalDate>? = when (period) {
    StatsPeriod.Last4Weeks -> today.minusDays(55) to today.minusDays(28)
    StatsPeriod.ThisMonth -> {
        val previous = YearMonth.from(today).minusMonths(1)
        previous.atDay(1) to previous.atEndOfMonth()
    }
    StatsPeriod.ThisYear -> {
        val previous = today.year - 1
        LocalDate.of(previous, 1, 1) to LocalDate.of(previous, 12, 31)
    }
    StatsPeriod.AllTime -> null
}

private class Tally {
    var plays = 0
    var ms = 0L
}

private data class Day(val date: LocalDate, val song: DaySong, val plays: Int, val ms: Long)

/** Todas as linhas do resumo entre [from] e [to] (inclusive), já com o tempo estimado somado. */
private fun daysIn(summary: DailySummary, from: LocalDate?, to: LocalDate, durationOf: (String) -> Long): List<Day> =
    summary.months.flatMap { month ->
        if ((from != null && month.month.atEndOfMonth().isBefore(from)) || month.month.atDay(1).isAfter(to)) return@flatMap emptyList()
        month.days.flatMap { (day, rows) ->
            val date = month.month.atDay(day.coerceIn(1, month.month.lengthOfMonth()))
            if ((from != null && date.isBefore(from)) || date.isAfter(to)) return@flatMap emptyList()
            rows.mapNotNull { row ->
                val song = month.songs.getOrNull(row.song) ?: return@mapNotNull null
                val estimated = row.estimatedPlays * durationOf.orDefault(song.uri)
                Day(date, song, row.plays, row.listenedMs + estimated)
            }
        }
    }

private fun minutesOf(ms: Long) = (ms / 60_000L).toInt()

/** Duração da música; sem ela (saiu do aparelho), 3 minutos. */
private fun ((String) -> Long).orDefault(uri: String): Long = invoke(uri).takeIf { it > 0 } ?: FALLBACK_DURATION_MS

private fun <K> rank(items: Map<K, Tally>, tieName: (K) -> String): List<Pair<K, Tally>> =
    items.entries.map { it.key to it.value }
        .sortedWith(compareByDescending<Pair<K, Tally>> { it.second.plays }.thenByDescending { it.second.ms }.thenBy { tieName(it.first) })

/** Os nomes de artista que aparecem nas linhas, juntando maiúsculas e minúsculas e guardando a grafia mais usada. */
private fun artistTallies(days: List<Day>): Pair<Map<String, Tally>, Map<String, String>> {
    val tallies = HashMap<String, Tally>()
    val spellings = HashMap<String, HashMap<String, Int>>()
    for (day in days) {
        val name = day.song.artist?.trim()?.takeIf { it.isNotEmpty() } ?: continue
        val key = name.lowercase()
        val tally = tallies.getOrPut(key) { Tally() }
        tally.plays += day.plays
        tally.ms += day.ms
        spellings.getOrPut(key) { HashMap() }.merge(name, 1) { a, b -> a + b }
    }
    return tallies to spellings.mapValues { (_, count) -> count.entries.maxWith(compareBy<Map.Entry<String, Int>> { it.value }.thenBy { it.key }).key }
}

/** Quanto subiu/desceu cada artista em relação ao período anterior. */
private fun changes(current: List<String>, previous: List<String>?): Map<String, RankChange> {
    if (previous == null || previous.isEmpty()) return current.associateWith { RankChange.None }
    val before = previous.withIndex().associate { it.value to it.index }
    return current.withIndex().associate { (index, key) ->
        val old = before[key]
        key to when {
            old == null -> RankChange.New
            old > index -> RankChange.Up(old - index)
            old < index -> RankChange.Down(index - old)
            else -> RankChange.Same
        }
    }
}

fun computeStatsReport(
    summary: DailySummary,
    period: StatsPeriod,
    today: LocalDate,
    durationOf: (String) -> Long,
): StatsReport {
    val (from, to) = periodRange(period, today)
    val days = daysIn(summary, from, to, durationOf)
    val includeLegacy = period == StatsPeriod.AllTime

    // Ranking de artistas do período anterior (só a ordem), para as setas.
    val previousOrder = previousRange(period, today)?.let { (pFrom, pTo) ->
        val (tallies, _) = artistTallies(daysIn(summary, pFrom, pTo, durationOf))
        rank(tallies) { it }.map { it.first }
    }

    // Artistas
    val (artistMap, spelling) = artistTallies(days).let { (tallies, names) ->
        if (!includeLegacy) return@let tallies to names
        val withLegacy = HashMap(tallies)
        val allNames = HashMap(names)
        for (song in summary.legacy) {
            val name = song.artist?.trim()?.takeIf { it.isNotEmpty() } ?: continue
            val key = name.lowercase()
            val tally = withLegacy.getOrPut(key) { Tally() }
            tally.plays += song.plays
            tally.ms += song.plays * durationOf.orDefault(song.uri)
            allNames.putIfAbsent(key, name)
        }
        withLegacy to allNames
    }
    val artistOrder = rank(artistMap) { it }
    val change = changes(artistOrder.map { it.first }.take(TOP_LIMIT), previousOrder)
    val artists = artistOrder.take(TOP_LIMIT).map { (key, tally) ->
        ArtistRank(spelling[key] ?: key, tally.plays, minutesOf(tally.ms), change[key] ?: RankChange.None)
    }

    // Músicas, álbuns e gêneros
    val songs = HashMap<String, Tally>()
    val songInfo = HashMap<String, DaySong>()
    val albums = HashMap<Pair<String, String>, Tally>()
    val albumInfo = HashMap<Pair<String, String>, Triple<String, String?, String>>()
    val genres = HashMap<String, Tally>()
    val genreName = HashMap<String, String>()

    fun add(song: DaySong, plays: Int, ms: Long) {
        songs.getOrPut(song.uri) { Tally() }.also { it.plays += plays; it.ms += ms }
        songInfo[song.uri] = song
        val album = song.album?.trim()?.takeIf { it.isNotEmpty() }
        if (album != null) {
            val key = album.lowercase() to song.artist.orEmpty().trim().lowercase()
            albums.getOrPut(key) { Tally() }.also { it.plays += plays; it.ms += ms }
            albumInfo.putIfAbsent(key, Triple(album, song.artist?.trim()?.takeIf { it.isNotEmpty() }, song.uri))
        }
        for (genre in song.genres) {
            val key = genre.lowercase()
            genres.getOrPut(key) { Tally() }.plays += plays
            genreName.putIfAbsent(key, genre)
        }
    }
    days.forEach { add(it.song, it.plays, it.ms) }
    if (includeLegacy) {
        for (song in summary.legacy) {
            add(DaySong(song.uri, song.title, song.artist, song.album, song.genres), song.plays, song.plays * durationOf.orDefault(song.uri))
        }
    }
    val songRanks = rank(songs) { songInfo[it]?.title?.lowercase().orEmpty() }.take(TOP_LIMIT).map { (uri, tally) ->
        val info = songInfo.getValue(uri)
        SongRank(uri, info.title, info.artist, tally.plays, minutesOf(tally.ms))
    }
    val albumRanks = rank(albums) { it.first }.take(TOP_LIMIT).map { (key, tally) ->
        val (album, artist, cover) = albumInfo.getValue(key)
        AlbumRank(album, artist, cover, tally.plays, minutesOf(tally.ms))
    }
    val genrePlays = genres.values.sumOf { it.plays }.coerceAtLeast(1)
    val genreRanks = rank(genres) { it }.take(TOP_LIMIT).map { (key, tally) ->
        GenreRank(genreName.getValue(key), tally.plays, tally.plays.toFloat() / genrePlays)
    }

    // Totais, dia recorde, sequência e dia com mais artistas (só o que tem data)
    val byDay = days.groupBy { it.date }.filterValues { rows -> rows.any { it.plays > 0 || it.ms > 0 } }
    val bestDay = byDay.entries.map { (date, rows) -> RecordDay(date, minutesOf(rows.sumOf { it.ms })) }
        .filter { it.minutes > 0 }
        .maxWithOrNull(compareBy<RecordDay> { it.minutes }.thenByDescending { it.date })
    val mostArtists = byDay.entries.map { (date, rows) ->
        MostArtistsDay(date, rows.filter { it.plays > 0 }.mapNotNull { it.song.artist?.trim()?.lowercase()?.takeIf(String::isNotEmpty) }.distinct().size)
    }.filter { it.artists > 0 }.maxWithOrNull(compareBy<MostArtistsDay> { it.artists }.thenByDescending { it.date })

    val legacyPlays = if (includeLegacy) summary.legacy.sumOf { it.plays } else 0
    val totalMs = days.sumOf { it.ms } + if (includeLegacy) summary.legacy.sumOf { it.plays * durationOf.orDefault(it.uri) } else 0L

    return StatsReport(
        period = period,
        from = from,
        to = to,
        plays = days.sumOf { it.plays } + legacyPlays,
        minutes = minutesOf(totalMs),
        activeDays = byDay.size,
        artistCount = artistMap.size,
        genreCount = genres.size,
        bestDay = bestDay,
        longestStreak = longestStreak(byDay.keys),
        mostArtistsDay = mostArtists,
        artists = artists,
        songs = songRanks,
        albums = albumRanks,
        genres = genreRanks,
        sprint = sprintOf(days, artists.take(SPRINT_TOP).map { it.name }),
        hasEstimated = (includeLegacy && summary.legacy.isNotEmpty()) || hasEstimatedRows(summary, from, to),
    )
}

private fun hasEstimatedRows(summary: DailySummary, from: LocalDate?, to: LocalDate): Boolean =
    summary.months.any { month ->
        month.days.any { (day, rows) ->
            val date = month.month.atDay(day.coerceIn(1, month.month.lengthOfMonth()))
            (from == null || !date.isBefore(from)) && !date.isAfter(to) && rows.any { it.estimatedPlays > 0 }
        }
    }

/** Maior número de dias seguidos com escuta. */
internal fun longestStreak(days: Collection<LocalDate>): Int {
    var best = 0
    var run = 0
    var last: LocalDate? = null
    for (day in days.sorted()) {
        run = if (last != null && last.plusDays(1) == day) run + 1 else 1
        best = maxOf(best, run)
        last = day
    }
    return best
}

/** O Top 5 mês a mês (os últimos 6 meses com escuta do período); só com 2 meses ou mais. */
private fun sprintOf(days: List<Day>, names: List<String>): Sprint? {
    if (names.isEmpty()) return null
    val months = days.map { YearMonth.from(it.date) }.distinct().sorted().takeLast(SPRINT_MONTHS)
    if (months.size < 2) return null
    val keys = names.map { it.lowercase() }
    val perMonth = months.map { month ->
        val (tallies, _) = artistTallies(days.filter { YearMonth.from(it.date) == month })
        rank(tallies) { it }.take(SPRINT_DEPTH).withIndex().associate { it.value.first to it.index + 1 }
    }
    return Sprint(months, names.indices.map { i -> SprintRow(names[i], perMonth.map { it[keys[i]] }) })
}
