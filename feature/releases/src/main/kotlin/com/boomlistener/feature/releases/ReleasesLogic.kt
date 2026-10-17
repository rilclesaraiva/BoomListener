// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.releases

import com.boomlistener.core.model.ExternalArtist
import com.boomlistener.core.model.Release
import com.boomlistener.core.model.ReleaseType
import java.time.LocalDate
import java.time.format.DateTimeParseException

/** A discografia de um artista seguido. */
internal data class ArtistReleases(val artist: ExternalArtist, val releases: List<Release>)

/** Um lançamento na lista "Mais recentes", com o artista dele. */
internal data class ReleaseItem(val artist: ExternalArtist, val release: Release)

internal enum class ReleaseFilter { All, Albums, EPs, Singles }

internal enum class ReleasesView { Latest, ByArtist }

internal fun ReleaseFilter.accepts(type: ReleaseType): Boolean = when (this) {
    ReleaseFilter.All -> true
    ReleaseFilter.Albums -> type == ReleaseType.Album
    ReleaseFilter.EPs -> type == ReleaseType.EP
    ReleaseFilter.Singles -> type == ReleaseType.Single
}

/**
 * Todos os lançamentos dos artistas seguidos, do mais novo para o mais antigo.
 * Um álbum com dois artistas seguidos aparece uma vez só.
 */
internal fun latestReleases(artists: List<ArtistReleases>, filter: ReleaseFilter): List<ReleaseItem> =
    artists.flatMap { entry -> entry.releases.map { ReleaseItem(entry.artist, it) } }
        .filter { filter.accepts(it.release.type) }
        .distinctBy { it.release.id }
        .sortedByDescending { it.release.releaseDate.orEmpty() }

/** Por artista: quem lançou algo mais recentemente vem primeiro; sem lançamento no filtro, some. */
internal fun releasesByArtist(artists: List<ArtistReleases>, filter: ReleaseFilter): List<ArtistReleases> =
    artists.map { entry -> entry.copy(releases = entry.releases.filter { filter.accepts(it.type) }) }
        .filter { it.releases.isNotEmpty() }
        .sortedByDescending { entry -> entry.releases.maxOfOrNull { it.releaseDate.orEmpty() }.orEmpty() }

/** Lançado nos últimos [days] dias (ou com data no futuro: pré-lançamento). */
internal fun isNewRelease(release: Release, today: LocalDate, days: Long = 30): Boolean {
    val date = release.releaseDate?.let {
        try {
            LocalDate.parse(it)
        } catch (_: DateTimeParseException) {
            null
        }
    } ?: return false
    return !date.isBefore(today.minusDays(days))
}
