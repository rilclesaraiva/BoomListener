// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

/** Artistas que o Deezer não tinha foto há pouco: ao voltar à tela não esperam de novo. */
private object ArtistPhotoMissing {
    private const val TTL_MS = 2 * 60_000L
    private val at = HashMap<String, Long>()

    @Synchronized fun add(key: String) { at[key] = System.currentTimeMillis() }

    @Synchronized fun recent(key: String): Boolean =
        at[key]?.let { System.currentTimeMillis() - it < TTL_MS } == true
}

/** A foto de um artista: [image] se já veio, [pending] se ainda está sendo buscada (aí nem a capa nem a inicial devem aparecer). */
internal class ArtistPhotoState(val image: ImageBitmap?, val pending: Boolean)

/**
 * A foto do artista [name] no Deezer (a mesma da página dele), ou sem imagem
 * (e [ArtistPhotoState.pending] falso) sem internet, com o modo offline ou se o
 * Deezer não o conhece: aí quem chama mostra a capa de uma música dele.
 */
@Composable
internal fun rememberArtistPhotoState(name: String?, size: Dp, load: suspend (String, Int) -> Bitmap?): ArtistPhotoState {
    val px = with(LocalDensity.current) { size.roundToPx() }
    val key = name?.let { "$it@$px" }
    val cached = key?.let { ArtistPhotoMemory.get(it) }
    var settled by remember(key) { mutableStateOf(key == null || cached != null || ArtistPhotoMissing.recent(key)) }
    val image by produceState(initialValue = cached, key) {
        if (name == null || key == null || value != null) return@produceState
        val bitmap = load(name, px)
        if (!isActive) {
            DiagnosticLog.log("foto", "carga da foto de artista cancelada ao sair da tela")
            return@produceState
        }
        if (bitmap == null) {
            DiagnosticLog.log("foto", "foto de artista sem imagem, ainda na tela")
            ArtistPhotoMissing.add(key)
        }
        value = bitmap?.asImageBitmap()?.also { ArtistPhotoMemory.put(key, it) }
        settled = true
    }
    return ArtistPhotoState(image, pending = !settled && image == null)
}

@Composable
internal fun rememberArtistPhoto(name: String?, size: Dp, load: suspend (String, Int) -> Bitmap?): ImageBitmap? =
    rememberArtistPhotoState(name, size, load).image
