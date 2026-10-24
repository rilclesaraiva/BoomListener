// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import com.boomlistener.core.model.Track

/**
 * As músicas cujas capas montam a capa de uma playlist: com mais de 3
 * músicas e pelo menos 4 álbuns diferentes, uma de cada um dos 4 primeiros
 * álbuns (mosaico 2 × 2); senão, só a primeira (capa única). Capas repetidas
 * no mosaico ficariam estranhas, por isso os álbuns têm que ser diferentes.
 */
internal fun mosaicCovers(tracks: List<Track>): List<Track> {
    if (tracks.size <= 3) return listOfNotNull(tracks.firstOrNull())
    val distinct = tracks.distinctBy { it.albumId }
    return if (distinct.size >= MOSAIC_SIZE) distinct.take(MOSAIC_SIZE) else listOfNotNull(tracks.firstOrNull())
}

internal const val MOSAIC_SIZE = 4
