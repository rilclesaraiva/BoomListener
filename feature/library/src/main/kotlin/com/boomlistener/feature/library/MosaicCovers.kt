// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import com.boomlistener.core.model.Track

/**
 * As músicas cujas capas montam a capa de uma playlist (mosaico 2 × 2).
 *
 * Com mais de 3 músicas e pelo menos 2 álbuns diferentes, monta o mosaico
 * com as capas dos álbuns diferentes, na ordem da playlist; com menos de 4
 * álbuns, as capas se repetem em diagonal (A B / B A, A B / C A), nunca lado a
 * lado. Até 3 músicas, ou todas do mesmo álbum, fica a capa única da primeira
 * (quatro capas iguais não formam um mosaico).
 */
internal fun mosaicCovers(tracks: List<Track>): List<Track> {
    val single = listOfNotNull(tracks.firstOrNull())
    if (tracks.size <= 3) return single
    val distinct = tracks.distinctBy { it.albumId }
    return when (distinct.size) {
        0, 1 -> single
        2 -> listOf(distinct[0], distinct[1], distinct[1], distinct[0])
        3 -> listOf(distinct[0], distinct[1], distinct[2], distinct[0])
        else -> distinct.take(MOSAIC_SIZE)
    }
}

internal const val MOSAIC_SIZE = 4
