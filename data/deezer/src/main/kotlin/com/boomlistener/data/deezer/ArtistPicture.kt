// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.deezer

import android.graphics.Bitmap
import com.boomlistener.core.common.DiagnosticLog

/** O código (MD5) do "vazio": o Deezer o usa no endereço da silhueta padrão de quem não tem foto. */
private const val EMPTY_MD5 = "d41d8cd98f00b204e9800998ecf8427e"

/** O código da silhueta padrão que o Deezer manda hoje (visto em artistas diferentes no registro de diagnóstico). */
private const val SILHOUETTE_CODE = "c4babb7727a1acd0b1ba07cfed913c92"

/**
 * O Deezer devolve, para artista sem foto, o endereço de uma silhueta padrão: o código
 * da imagem no endereço vem vazio (".../images/artist//1000x1000...") ou é o MD5 do
 * vazio. Isso não é foto: quem mostra usa a capa do lançamento mais novo no lugar.
 */
fun isDefaultArtistPicture(url: String): Boolean = url.contains("/images/artist//") || url.contains(EMPTY_MD5) || url.contains(SILHOUETTE_CODE)

/** O formato do código da imagem no endereço (só o tamanho dele, sem o endereço), para o registro de diagnóstico. */
private fun pictureShape(url: String): String {
    val code = url.substringAfter("/images/artist/", "").substringBefore('/')
    return if (code.isEmpty()) "sem código" else "código de ${code.length} caracteres"
}

/** Código da imagem → ids dos artistas que o usam (só na memória, para o registro de diagnóstico). */
private val codesSeen = HashMap<String, MutableSet<Long>>()

/**
 * Uma foto de verdade é única de cada artista; um código que aparece em artistas diferentes é
 * quase certamente uma imagem padrão. Registra esse código (o código de uma imagem não
 * identifica ninguém) para a silhueta poder ser reconhecida pelo endereço.
 */
private fun noteCode(url: String, artistId: Long) {
    val code = url.substringAfter("/images/artist/", "").substringBefore('/')
    if (code.isEmpty()) return
    val artists = synchronized(codesSeen) { codesSeen.getOrPut(code) { HashSet() }.apply { add(artistId) }.size }
    if (artists == 2) DiagnosticLog.log("foto", "o mesmo código de imagem em 2 artistas diferentes: $code")
}

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
    if (isDefaultArtistPicture(url)) {
        DiagnosticLog.log("foto", "sem foto: o Deezer só tem a silhueta padrão (${pictureShape(url)})")
        return null
    }
    noteCode(url, found.id)
    DiagnosticLog.log("foto", "foto do Deezer aceita (${pictureShape(url)})")
    return image(url, sizePx).also { if (it == null) DiagnosticLog.log("foto", "sem foto: a imagem não veio") }
}

/**
 * A capa do lançamento mais novo do artista no Deezer: o que a página dele já usa no
 * círculo quando ele não tem foto. Nulo se não achar o artista, se ele não tem
 * lançamentos ou sem internet.
 */
suspend fun DeezerRepository.artistLatestCover(name: String, sizePx: Int): Bitmap? {
    val found = pickArtist(name, searchArtists(primaryArtistName(name)) ?: return null)
    if (found == null) {
        DiagnosticLog.log("foto", "sem capa: artista não achado no Deezer")
        return null
    }
    val url = latestReleaseCoverUrl(found.id)
    if (url == null) {
        DiagnosticLog.log("foto", "sem capa: o Deezer não deu lançamentos")
        return null
    }
    return image(url, sizePx).also { if (it == null) DiagnosticLog.log("foto", "sem capa: a imagem do lançamento não veio") }
}
