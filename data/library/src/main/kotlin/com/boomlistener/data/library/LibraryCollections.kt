// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.library

import com.boomlistener.core.common.normalizedForSearch
import com.boomlistener.core.model.Track
import java.text.Collator
import java.util.Locale

/*
 * A biblioteca separada em gêneros, artistas e álbuns, a partir das tags.
 * Puro Kotlin e testado (acentos e maiúsculas não separam grupos).
 */

/** Um grupo de músicas (gênero, artista ou álbum). [key] identifica o grupo na navegação. */
data class TrackGroup(
    val key: String,
    val name: String,
    /** Artista do álbum (só em álbuns). */
    val subtitle: String?,
    val year: Int?,
    val tracks: List<Track>,
)

private val FEATURING = Regex("""\s+(feat\.?|ft\.?|featuring)\s+.*$""", RegexOption.IGNORE_CASE)

private fun collator(): Collator = Collator.getInstance(Locale.forLanguageTag("pt-BR")).apply {
    strength = Collator.PRIMARY
}

/**
 * Gêneros. Uma música com "Rock; Pop" entra nos dois. "rock" e "Rock" são o
 * mesmo gênero (vale a grafia mais comum).
 */
fun groupByGenre(tracks: List<Track>): List<TrackGroup> =
    groupBy(tracks) { track ->
        track.genre?.split(';')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
    }

/** Artistas, pelo artista principal ("Djavan feat. Caetano" fica em Djavan). */
fun groupByArtist(tracks: List<Track>): List<TrackGroup> =
    groupBy(tracks) { track ->
        listOfNotNull(track.artist?.replace(FEATURING, "")?.trim()?.takeIf { it.isNotEmpty() })
    }

/** Álbuns, pelo álbum do MediaStore; músicas em ordem de faixa. */
fun groupByAlbum(tracks: List<Track>): List<TrackGroup> {
    val collator = collator()
    return tracks.filter { !it.album.isNullOrBlank() }
        .groupBy { it.albumId }
        .map { (albumId, albumTracks) ->
            val artist = albumTracks.mapNotNull { it.artist }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
            TrackGroup(
                key = albumId.toString(),
                name = albumTracks.first().album.orEmpty(),
                subtitle = artist,
                year = albumTracks.mapNotNull { it.year }.maxOrNull(),
                tracks = albumTracks.sortedWith(
                    compareBy<Track> { it.trackNumber ?: Int.MAX_VALUE }.thenComparator { a, b -> collator.compare(a.title, b.title) },
                ),
            )
        }
        .sortedWith { a, b -> collator.compare(a.name, b.name) }
}

private fun groupBy(tracks: List<Track>, namesOf: (Track) -> List<String>): List<TrackGroup> {
    val collator = collator()
    val buckets = LinkedHashMap<String, MutableList<Pair<String, Track>>>()
    for (track in tracks) {
        for (name in namesOf(track).distinctBy { it.normalizedForSearch() }) {
            buckets.getOrPut(name.normalizedForSearch()) { mutableListOf() } += name to track
        }
    }
    return buckets.map { (key, entries) ->
        val name = entries.groupingBy { it.first }.eachCount().maxByOrNull { it.value }!!.key
        TrackGroup(
            key = key,
            name = name,
            subtitle = null,
            year = null,
            tracks = entries.map { it.second }.sortedWith { a, b -> collator.compare(a.title, b.title) },
        )
    }.sortedWith { a, b -> collator.compare(a.name, b.name) }
}
