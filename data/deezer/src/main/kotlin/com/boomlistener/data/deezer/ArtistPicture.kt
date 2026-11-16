// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.deezer

import android.graphics.Bitmap
import com.boomlistener.core.common.DiagnosticLog

/**
 * Foto de um artista pelo nome: o mesmo artista que a página dele acharia
 * ([pickArtist]: nome igual; entre homônimos, o mais conhecido). Nulo se não
 * achar, sem foto ou sem internet (aí quem mostra usa a inicial).
 */
suspend fun DeezerRepository.artistPicture(name: String, sizePx: Int): Bitmap? {
    val results = searchArtists(primaryArtistName(name))
    if (results == null) {
        DiagnosticLog.log("foto", "sem foto: a busca devolveu nulo")
        return null
    }
    val found = pickArtist(name, results)
    if (found == null) {
        DiagnosticLog.log("foto", "sem foto: nenhum artista igual entre ${results.size} resultados")
        return null
    }
    val url = found.pictureUrl
    if (url == null) {
        DiagnosticLog.log("foto", "sem foto: o artista achado não tem imagem")
        return null
    }
    return image(url, sizePx).also { if (it == null) DiagnosticLog.log("foto", "sem foto: a imagem não veio") }
}

/**
 * A capa do lançamento mais novo do artista no Deezer: o que a página dele já usa no
 * círculo quando ele não tem foto. Nulo se não achar o artista, se ele não tem
 * lançamentos ou sem internet.
 */
suspend fun DeezerRepository.artistLatestCover(name: String, sizePx: Int): Bitmap? {
    val found = pickArtist(name, searchArtists(primaryArtistName(name)) ?: return null) ?: return null
    val releases = (artistReleases(found.id) as? ArtistReleasesResult.Found)?.releases ?: return null
    val url = releases.maxByOrNull { it.releaseDate.orEmpty() }?.coverUrl ?: return null
    return image(url, sizePx)
}
