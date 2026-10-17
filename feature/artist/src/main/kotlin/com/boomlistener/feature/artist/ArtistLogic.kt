// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.artist

import com.boomlistener.core.common.normalizedForSearch
import com.boomlistener.core.model.PopularTrack
import com.boomlistener.core.model.Release
import com.boomlistener.core.model.Track
import com.boomlistener.data.deezer.comparableTitle
import com.boomlistener.data.deezer.primaryArtistName

/** Músicas da biblioteca que são deste artista (inclui "feat." dele mesmo). */
internal fun tracksOfArtist(allTracks: List<Track>, artistName: String): List<Track> {
    val wanted = primaryArtistName(artistName).normalizedForSearch()
    return allTracks.filter { track ->
        track.artist?.let { primaryArtistName(it).normalizedForSearch() } == wanted
    }
}

/** Quantos álbuns diferentes do artista existem na biblioteca. */
internal fun localAlbumCount(tracks: List<Track>): Int =
    tracks.mapNotNull { it.album?.let(::comparableTitle) }.filter { it.isNotEmpty() }.distinct().size

/**
 * Lançamentos que você já tem: o nome do álbum de alguma música sua bate com
 * o do lançamento (ignorando "(Deluxe)", "- Single", acentos…). Um single
 * também conta se você tem a música de mesmo nome.
 */
internal fun ownedReleaseIds(releases: List<Release>, localTracks: List<Track>): Set<Long> {
    val albums = localTracks.mapNotNull { it.album?.let(::comparableTitle) }.toSet()
    val titles = localTracks.map { comparableTitle(it.title) }.toSet()
    return releases.filter { release ->
        val title = comparableTitle(release.title)
        title in albums || (release.type == com.boomlistener.core.model.ReleaseType.Single && title in titles)
    }.map { it.id }.toSet()
}

/** Músicas populares que você já tem (mesmo título, ignorando versões e acentos). */
internal fun ownedPopularIds(top: List<PopularTrack>, localTracks: List<Track>): Set<Long> {
    val titles = localTracks.map { comparableTitle(it.title) }.toSet()
    return top.filter { comparableTitle(it.title) in titles }.map { it.id }.toSet()
}
