// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.deezer

import android.graphics.Bitmap

/**
 * Foto de um artista pelo nome: o mesmo artista que a página dele acharia
 * ([pickArtist]: nome igual; entre homônimos, o mais conhecido). Nulo se não
 * achar, sem foto ou sem internet (aí quem mostra usa a inicial).
 */
suspend fun DeezerRepository.artistPicture(name: String, sizePx: Int): Bitmap? {
    val found = searchArtists(primaryArtistName(name))?.let { pickArtist(name, it) } ?: return null
    val url = found.pictureUrl ?: return null
    return image(url, sizePx)
}
