// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.profile

import com.boomlistener.data.wellbeing.HistoryEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/*
 * Os números do seu perfil, calculados no aparelho a partir do histórico de
 * escuta (puro e testado). Nada disso sai do aparelho.
 */

/** Parte do dia em que você mais ouve. */
internal enum class DayPart { Dawn, Morning, Afternoon, Night }

internal fun dayPartOf(hour: Int): DayPart = when (hour) {
    in 0..5 -> DayPart.Dawn
    in 6..11 -> DayPart.Morning
    in 12..17 -> DayPart.Afternoon
    else -> DayPart.Night
}

internal data class ArtistCount(val name: String, val plays: Int)

internal data class DayMinutes(val date: LocalDate, val minutes: Int)

internal data class ProfileStats(
    /** Reproduções neste mês. */
    val monthPlays: Int,
    /** Minutos ouvidos neste mês (pela duração das músicas). */
    val monthMinutes: Int,
    /** Artistas diferentes neste mês. */
    val monthArtists: Int,
    /** Os últimos 7 dias, do mais antigo para hoje. */
    val week: List<DayMinutes>,
    /** De 0 a 1: quanto de cada parte do dia (últimos 30 dias), relativo à maior. */
    val dayParts: Map<DayPart, Float>,
    /** Artistas mais ouvidos nos últimos 30 dias. */
    val topArtists: List<ArtistCount>,
)

/**
 * [durations] é a duração (ms) de cada música pelo endereço; música que não
 * está mais no aparelho conta 3 minutos.
 */
internal fun computeProfileStats(
    history: List<HistoryEntry>,
    durations: Map<String, Long>,
    today: LocalDate,
    zone: ZoneId,
): ProfileStats {
    fun dateOf(entry: HistoryEntry) = Instant.ofEpochMilli(entry.timestampMs).atZone(zone).toLocalDate()
    fun minutesOf(entry: HistoryEntry) = ((durations[entry.uri] ?: 180_000L) / 60_000.0)

    val monthStart = today.withDayOfMonth(1)
    val thisMonth = history.filter { !dateOf(it).isBefore(monthStart) && !dateOf(it).isAfter(today) }

    val weekStart = today.minusDays(6)
    val byDay = history.filter { !dateOf(it).isBefore(weekStart) && !dateOf(it).isAfter(today) }
        .groupBy { dateOf(it) }
    val week = (0L..6L).map { offset ->
        val day = weekStart.plusDays(offset)
        DayMinutes(day, byDay[day].orEmpty().sumOf { minutesOf(it) }.toInt())
    }

    val last30 = history.filter { !dateOf(it).isBefore(today.minusDays(29)) && !dateOf(it).isAfter(today) }
    val partCounts = last30.groupingBy { dayPartOf(Instant.ofEpochMilli(it.timestampMs).atZone(zone).hour) }.eachCount()
    val maxPart = partCounts.values.maxOrNull() ?: 0
    val dayParts = DayPart.entries.associateWith { part ->
        if (maxPart == 0) 0f else (partCounts[part] ?: 0).toFloat() / maxPart
    }

    // "Djavan" e "djavan" são o mesmo artista; vale a grafia mais usada.
    val topArtists = last30.mapNotNull { it.artist?.trim()?.takeIf(String::isNotEmpty) }
        .groupBy { it.lowercase() }
        .map { (_, names) ->
            ArtistCount(names.groupingBy { it }.eachCount().maxByOrNull { it.value }!!.key, names.size)
        }
        .sortedWith(compareByDescending<ArtistCount> { it.plays }.thenBy { it.name.lowercase() })
        .take(10)

    return ProfileStats(
        monthPlays = thisMonth.size,
        monthMinutes = thisMonth.sumOf { minutesOf(it) }.toInt(),
        monthArtists = thisMonth.mapNotNull { it.artist?.lowercase() }.distinct().size,
        week = week,
        dayParts = dayParts,
        topArtists = topArtists,
    )
}
