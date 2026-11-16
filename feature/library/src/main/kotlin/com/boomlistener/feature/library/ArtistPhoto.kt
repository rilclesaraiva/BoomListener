// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.boomlistener.core.common.DiagnosticLog
import com.boomlistener.core.designsystem.component.PinImages
import com.boomlistener.core.designsystem.component.PinItem
import com.boomlistener.core.designsystem.component.rememberImageSet
import com.boomlistener.data.deezer.splitArtistNames
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

/** Chave da foto do artista [name] no tamanho [px] na memória de imagens. */
internal fun artistPhotoKey(name: String, px: Int) = "artist:$name@$px"

/** Chave da imagem de [name] que, sem foto, cai na capa do último lançamento ([version]: sobe com as tags). */
internal fun artistOrCoverKey(name: String, px: Int, version: Int) = "artistOrCover:$name@$px#$version"

/** Carrega a imagem de um artista; [coverFallback]: sem foto no Deezer, a capa do último lançamento dele no aparelho. */
internal typealias ArtistImageLoader = suspend (name: String, px: Int, coverFallback: Boolean) -> Bitmap?

/** Chave da capa de [uri] no tamanho [px] ([version]: sobe quando as tags mudam e a capa é lida de novo). */
internal fun coverKey(uri: String, px: Int, version: Int) = "cover:$uri@$px#$version"

/**
 * As imagens de [name] por tentativas. Um artista só: a foto dele ou, sem foto, a
 * capa do último lançamento dele. Tag com vários ("Delinquent, KCAT"): a foto do
 * nome inteiro primeiro (acerta bandas como "Earth, Wind & Fire"); senão, a de
 * cada artista (foto, ou a capa do último lançamento de quem não tem).
 */
internal fun artistAttempts(name: String, px: Int, version: Int, load: ArtistImageLoader): List<List<PinItem>> {
    fun withCover(n: String) = PinItem(artistOrCoverKey(n, px, version)) { loadLogged(n, px, true, load) }
    val parts = splitArtistNames(name).takeIf { it.size > 1 } ?: return listOf(listOf(withCover(name)))
    val whole = PinItem(artistPhotoKey(name, px)) { loadLogged(name, px, false, load) }
    return listOf(listOf(whole), parts.map { withCover(it) })
}

/**
 * As imagens do artista [name] (uma, ou a de cada artista de uma tag com vários):
 * [com.boomlistener.core.designsystem.component.BoomImageCycle] as alterna.
 */
@Composable
internal fun rememberArtistPhotos(name: String?, size: Dp, version: Int, load: ArtistImageLoader): List<ImageBitmap> {
    val px = with(LocalDensity.current) { size.roundToPx() }
    return if (name == null) emptyList() else rememberImageSet(remember(name, px, version) { artistAttempts(name, px, version, load) })
}

/**
 * Carrega, uma de cada vez, a imagem dos [names] (os primeiros da lista) e as
 * deixa fixas na memória: tocar na aba para subir ao topo cai sempre perto
 * dele, e essas imagens já estão prontas quando a lista chega.
 */
@Composable
internal fun PinTopArtistPhotos(names: List<String>, size: Dp, version: Int, load: ArtistImageLoader) {
    val px = with(LocalDensity.current) { size.roundToPx() }
    PinImages("artists", names.flatMap { artistPinItems(it, px, version, load) }.distinctBy { it.key })
}

/** O que deixar fixo para [name]: a foto inteira e a de cada artista, se tiver vários. */
internal fun artistPinItems(name: String, px: Int, version: Int, load: ArtistImageLoader): List<PinItem> =
    artistAttempts(name, px, version, load).flatten()

private suspend fun loadLogged(name: String, px: Int, coverFallback: Boolean, load: ArtistImageLoader): Bitmap? {
    val bitmap = load(name, px, coverFallback)
    if (!currentCoroutineContext().isActive) {
        DiagnosticLog.log("foto", "carga da foto de artista cancelada ao sair da tela")
    } else if (bitmap == null) {
        DiagnosticLog.log("foto", "foto de artista sem imagem, ainda na tela")
    }
    return bitmap
}
