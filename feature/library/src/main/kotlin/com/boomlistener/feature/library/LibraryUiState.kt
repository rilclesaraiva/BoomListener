// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import com.boomlistener.core.common.normalizedForSearch
import com.boomlistener.core.model.Track

internal sealed interface LibraryUiState {
    data object Loading : LibraryUiState

    /**
     * @param tracks as músicas visíveis (já filtradas pela busca).
     * @param totalCount quantas músicas existem no aparelho, sem filtro.
     */
    data class Success(
        val tracks: List<Track>,
        val totalCount: Int,
        val query: String,
        /** Sobe quando tags foram editadas (para recarregar as capas). */
        val version: Int = 0,
    ) : LibraryUiState
}

/** Filtra por título, artista ou álbum, ignorando acentos e maiúsculas. */
internal fun filterTracks(tracks: List<Track>, query: String): List<Track> {
    val needle = query.normalizedForSearch()
    if (needle.isEmpty()) return tracks
    return tracks.filter { track ->
        listOfNotNull(track.title, track.artist, track.album)
            .any { it.normalizedForSearch().contains(needle) }
    }
}

/** 215000 ms → "3:35"; acima de uma hora, "1:02:05". */
internal fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

/**
 * Duração total (de um álbum ou playlist) em horas e minutos. Os segundos
 * que sobram arredondam para cima: 40 s viram "1 min", não "0 min".
 */
internal fun hoursAndMinutes(totalMs: Long): Pair<Long, Long> {
    val minutes = (totalMs.coerceAtLeast(0) + 59_999) / 60_000
    return minutes / 60 to minutes % 60
}
