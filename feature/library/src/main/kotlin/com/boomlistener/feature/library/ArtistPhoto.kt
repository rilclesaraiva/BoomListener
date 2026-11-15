// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.boomlistener.core.common.DiagnosticLog
import kotlinx.coroutines.isActive

/**
 * As fotos de artista já carregadas nesta abertura do app: rolar a lista e voltar
 * não mostra a capa de novo antes da foto. O teto é 1/8 da memória que o Android
 * dá ao app (a foto grande da grade ocupa mais de 1 MB; com 8 MB cabiam só umas
 * cinco, e a grade de artistas recarregava tudo ao voltar ao topo).
 */
private object ArtistPhotoMemory : LruCache<String, ImageBitmap>(
    (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt(),
) {
    override fun sizeOf(key: String, value: ImageBitmap): Int = (value.width * value.height * 4 / 1024).coerceAtLeast(1)

    override fun entryRemoved(evicted: Boolean, key: String, oldValue: ImageBitmap, newValue: ImageBitmap?) {
        if (evicted) DiagnosticLog.log("foto", "foto saiu da memória por falta de espaço (${size() / 1024} de ${maxSize() / 1024} MB)")
    }
}

/**
 * A foto do artista [name] no Deezer (a mesma da página dele), ou null sem
 * internet, com o modo offline ou se o Deezer não o conhece: aí quem chama
 * mostra a capa de uma música dele.
 */
@Composable
internal fun rememberArtistPhoto(name: String?, size: Dp, load: suspend (String, Int) -> Bitmap?): ImageBitmap? {
    val px = with(LocalDensity.current) { size.roundToPx() }
    val key = name?.let { "$it@$px" }
    val image by produceState(initialValue = key?.let { ArtistPhotoMemory.get(it) }, key) {
        if (name == null || key == null || value != null) return@produceState
        val bitmap = load(name, px)
        if (!isActive) {
            DiagnosticLog.log("foto", "carga da foto de artista cancelada ao sair da tela")
            return@produceState
        }
        if (bitmap == null) DiagnosticLog.log("foto", "foto de artista sem imagem, ainda na tela")
        value = bitmap?.asImageBitmap()?.also { ArtistPhotoMemory.put(key, it) }
    }
    return image
}
