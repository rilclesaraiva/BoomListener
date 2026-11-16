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

/** Chave da capa de [uri] no tamanho [px] ([version]: sobe quando as tags mudam e a capa é lida de novo). */
internal fun coverKey(uri: String, px: Int, version: Int) = "cover:$uri@$px#$version"

/**
 * As fotos de [name] por tentativas: o nome inteiro primeiro (acerta bandas como
 * "Earth, Wind & Fire"); se o Deezer não o conhece e a tag tem vários artistas
 * ("Delinquent, Kcat"), a de cada um.
 */
internal fun artistAttempts(name: String, px: Int, load: suspend (String, Int) -> Bitmap?): List<List<PinItem>> {
    val whole = listOf(PinItem(artistPhotoKey(name, px)) { loadLogged(name, px, load) })
    val parts = splitArtistNames(name).takeIf { it.size > 1 }
        ?.map { part -> PinItem(artistPhotoKey(part, px)) { loadLogged(part, px, load) } }
    return if (parts == null) listOf(whole) else listOf(whole, parts)
}

/**
 * As fotos do artista [name] (uma, ou a de cada artista de uma tag com vários):
 * [com.boomlistener.core.designsystem.component.BoomImageCycle] as alterna.
 * Vazia sem internet, no modo offline ou se o Deezer não conhece ninguém.
 */
@Composable
internal fun rememberArtistPhotos(name: String?, size: Dp, load: suspend (String, Int) -> Bitmap?): List<ImageBitmap> {
    val px = with(LocalDensity.current) { size.roundToPx() }
    return if (name == null) emptyList() else rememberImageSet(remember(name, px) { artistAttempts(name, px, load) })
}

/**
 * Carrega, uma de cada vez, a foto dos [names] (os primeiros da lista) e as
 * deixa fixas na memória: tocar na aba para subir ao topo cai sempre perto
 * dele, e essas fotos já estão prontas quando a lista chega.
 */
@Composable
internal fun PinTopArtistPhotos(names: List<String>, size: Dp, load: suspend (String, Int) -> Bitmap?) {
    val px = with(LocalDensity.current) { size.roundToPx() }
    PinImages("artists", names.flatMap { artistPinItems(it, px, load) }.distinctBy { it.key })
}

/** O que deixar fixo para [name]: a foto inteira e a de cada artista, se tiver vários. */
internal fun artistPinItems(name: String, px: Int, load: suspend (String, Int) -> Bitmap?): List<PinItem> =
    artistAttempts(name, px, load).flatten()

private suspend fun loadLogged(name: String, px: Int, load: suspend (String, Int) -> Bitmap?): Bitmap? {
    val bitmap = load(name, px)
    if (!currentCoroutineContext().isActive) {
        DiagnosticLog.log("foto", "carga da foto de artista cancelada ao sair da tela")
    } else if (bitmap == null) {
        DiagnosticLog.log("foto", "foto de artista sem imagem, ainda na tela")
    }
    return bitmap
}
