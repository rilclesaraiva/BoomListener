// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.boomlistener.core.common.DiagnosticLog
import com.boomlistener.core.designsystem.component.PinImages
import com.boomlistener.core.designsystem.component.PinItem
import com.boomlistener.core.designsystem.component.rememberMemoryImage
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

/** Chave da foto do artista [name] no tamanho [px] na memória de imagens. */
internal fun artistPhotoKey(name: String, px: Int) = "artist:$name@$px"

/** Chave da capa de [uri] no tamanho [px] ([version]: sobe quando as tags mudam e a capa é lida de novo). */
internal fun coverKey(uri: String, px: Int, version: Int) = "cover:$uri@$px#$version"

/**
 * A foto do artista [name] no Deezer (a mesma da página dele), ou null sem
 * internet, com o modo offline ou se o Deezer não o conhece: aí quem chama
 * mostra a capa de uma música dele. Fica na memória de imagens: voltar à tela
 * já mostra a foto, sem passar pela capa ou pela inicial.
 */
@Composable
internal fun rememberArtistPhoto(name: String?, size: Dp, load: suspend (String, Int) -> Bitmap?): ImageBitmap? {
    val px = with(LocalDensity.current) { size.roundToPx() }
    return rememberMemoryImage(name?.let { artistPhotoKey(it, px) }) { loadLogged(name!!, px, load) }
}

/**
 * Carrega, uma de cada vez, a foto dos [names] (os primeiros da lista) e as
 * deixa fixas na memória: tocar na aba para subir ao topo cai sempre perto
 * dele, e essas fotos já estão prontas quando a lista chega.
 */
@Composable
internal fun PinTopArtistPhotos(names: List<String>, size: Dp, load: suspend (String, Int) -> Bitmap?) {
    val px = with(LocalDensity.current) { size.roundToPx() }
    PinImages("artists", names.map { name -> PinItem(artistPhotoKey(name, px)) { load(name, px) } })
}

private suspend fun loadLogged(name: String, px: Int, load: suspend (String, Int) -> Bitmap?): Bitmap? {
    val bitmap = load(name, px)
    if (!currentCoroutineContext().isActive) {
        DiagnosticLog.log("foto", "carga da foto de artista cancelada ao sair da tela")
    } else if (bitmap == null) {
        DiagnosticLog.log("foto", "foto de artista sem imagem, ainda na tela")
    }
    return bitmap
}
