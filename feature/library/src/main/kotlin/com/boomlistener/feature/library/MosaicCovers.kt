// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import com.boomlistener.core.model.Track

/**
 * As músicas cujas capas montam a capa de uma playlist: com 4 ou mais álbuns
 * diferentes, o mosaico 2 × 2 com uma capa de cada um dos 4 primeiros (na
 * ordem da playlist, sem repetir). Com menos de 4 álbuns, a capa única da
 * primeira música (as formas de 2 e 3 capas saíram na 0.51.1: não ficaram boas).
 */
internal fun mosaicCovers(tracks: List<Track>): List<Track> {
    val distinct = tracks.distinctBy { it.albumId }
    return if (distinct.size >= MOSAIC_SIZE) distinct.take(MOSAIC_SIZE) else listOfNotNull(tracks.firstOrNull())
}

internal const val MOSAIC_SIZE = 4
