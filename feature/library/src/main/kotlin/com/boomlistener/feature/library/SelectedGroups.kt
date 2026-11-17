// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import com.boomlistener.core.model.Track

/**
 * As músicas dos grupos marcados (gêneros, artistas, playlists, álbuns), cada uma uma
 * vez só: a mesma música em dois grupos (um álbum e o artista dele, por exemplo) não
 * entra duas vezes na rádio. Mantém a ordem em que aparecem.
 */
internal fun distinctTracks(groups: List<List<Track>>): List<Track> =
    groups.flatten().distinctBy { it.uri }
