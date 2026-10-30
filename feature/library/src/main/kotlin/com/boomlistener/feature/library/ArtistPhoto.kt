// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
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

/**
 * As fotos de artista já carregadas nesta abertura do app (até ~8 MB): rolar a
 * lista e voltar não mostra a capa de novo antes da foto.
 */
private object ArtistPhotoMemory : LruCache<String, ImageBitmap>(8 * 1024) {
    override fun sizeOf(key: String, value: ImageBitmap): Int = (value.width * value.height * 4 / 1024).coerceAtLeast(1)
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
        value = load(name, px)?.asImageBitmap()?.also { ArtistPhotoMemory.put(key, it) }
    }
    return image
}
