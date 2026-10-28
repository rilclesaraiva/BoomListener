// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import com.boomlistener.core.model.Track

/**
 * As músicas cujas capas montam a capa de uma playlist: uma por álbum
 * diferente, na ordem da playlist, sem repetir capa nenhuma.
 *
 * - 1 álbum (ou vazia): a capa única da primeira música.
 * - 2 álbuns: as duas lado a lado, cortadas na vertical.
 * - 3 álbuns: em "Y", uma em cima e duas dos lados.
 * - 4 ou mais: o mosaico 2 × 2 com as quatro primeiras.
 *
 * O desenho de cada forma fica no `BoomMosaicArtwork`.
 */
internal fun mosaicCovers(tracks: List<Track>): List<Track> =
    tracks.distinctBy { it.albumId }.take(MOSAIC_SIZE).ifEmpty { listOfNotNull(tracks.firstOrNull()) }

internal const val MOSAIC_SIZE = 4

/** A partir de quantas capas diferentes a playlist ganha a capa montada. */
internal const val MOSAIC_MIN = 2
