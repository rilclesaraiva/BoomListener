// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.deezer

import android.graphics.Bitmap

/** O código (MD5) do "vazio": o Deezer o usa no endereço da silhueta padrão de quem não tem foto. */
private const val EMPTY_MD5 = "d41d8cd98f00b204e9800998ecf8427e"

/** Os códigos das silhuetas padrão que o Deezer manda hoje. */
private val SILHOUETTE_CODES = listOf("c4babb7727a1acd0b1ba07cfed913c92", "f0f2ae4b9f95cc09b9e3e9663569f2ce")

/**
 * O Deezer devolve, para artista sem foto, o endereço de uma silhueta padrão: o código
 * da imagem no endereço vem vazio (".../images/artist//1000x1000...") ou é o MD5 do
 * vazio. Isso não é foto: quem mostra usa a capa do lançamento mais novo no lugar.
 */
fun isDefaultArtistPicture(url: String): Boolean = url.contains("/images/artist//") || url.contains(EMPTY_MD5) || SILHOUETTE_CODES.any { url.contains(it) }

/**
 * Foto de um artista pelo nome: o mesmo artista que a página dele acharia
 * ([pickArtist]: nome igual; entre homônimos, o mais conhecido). Nulo se não
 * achar, sem foto ou sem internet (aí quem mostra usa a inicial).
 */
suspend fun DeezerRepository.artistPicture(name: String, sizePx: Int): Bitmap? {
    val results = searchArtists(primaryArtistName(name)) ?: return null
    val url = pickArtist(name, results)?.pictureUrl ?: return null
    if (isDefaultArtistPicture(url)) return null
    return image(url, sizePx)
}

/**
 * A capa do lançamento mais novo do artista no Deezer: o que a página dele já usa no
 * círculo quando ele não tem foto. Nulo se não achar o artista, se ele não tem
 * lançamentos ou sem internet.
 */
suspend fun DeezerRepository.artistLatestCover(name: String, sizePx: Int): Bitmap? {
    val found = pickArtist(name, searchArtists(primaryArtistName(name)) ?: return null) ?: return null
    val url = latestReleaseCoverUrl(found.id) ?: return null
    return image(url, sizePx)
}
