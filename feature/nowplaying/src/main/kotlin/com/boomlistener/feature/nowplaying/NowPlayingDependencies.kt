// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import android.util.LruCache
import androidx.compose.runtime.Composable
import com.boomlistener.core.designsystem.component.rememberArtworkColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.boomlistener.core.player.PlayerConnection
import com.boomlistener.data.library.LibraryData
import com.boomlistener.data.lyrics.LyricsData
import com.boomlistener.data.tags.TagsData
import com.boomlistener.data.deezer.DeezerData
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
internal fun nowPlayingViewModel(): NowPlayingViewModel {
    val context = LocalContext.current.applicationContext
    return viewModel {
        NowPlayingViewModel(
            PlayerConnection.controller(context),
            LibraryData.repository(context),
            LyricsData.repository(context),
            embeddedLyrics = TagsData.repository(context)::readLyrics,
            songLink = DeezerData.repository(context)::youtubeMusicLinkForSong,
        )
    }
}

/** Capa da música [uri] em ~[sizePx], carregada fora da thread da tela. */
@Composable
internal fun rememberArtwork(viewModel: NowPlayingViewModel, uri: String?, sizePx: Int): ImageBitmap? {
    // Muda quando alguma tag foi editada: a capa é lida de novo.
    val version by viewModel.libraryVersion.collectAsStateWithLifecycle()
    val cacheKey = "$uri|$sizePx|$version"
    // Capa já vista começa pronta (da memória), sem passar pela inicial: ao
    // trocar a fila (ex.: aleatório), o carrossel monta a página de novo e a
    // letra inicial piscava por um instante antes da capa carregar.
    val artwork by produceState<ImageBitmap?>(initialValue = uri?.let { ArtworkMemory.get(cacheKey) }, uri, sizePx, version) {
        val cached = uri?.let { ArtworkMemory.get(cacheKey) }
        value = cached ?: uri?.let { viewModel.artwork(it, sizePx)?.asImageBitmap() }?.also { ArtworkMemory.put(cacheKey, it) }
    }
    return artwork
}

/** As últimas capas mostradas no player e no mini player (até ~24 MB). */
private object ArtworkMemory : LruCache<String, ImageBitmap>(24 * 1024) {
    override fun sizeOf(key: String, value: ImageBitmap): Int = (value.width * value.height * 4 / 1024).coerceAtLeast(1)
}

/**
 * Cor da capa que tinge o mini player, o player aberto e o cartão que cresce
 * entre os dois. Sempre tirada da mesma capa pequena, para os três baterem.
 */
@Composable
internal fun rememberPlayerTint(viewModel: NowPlayingViewModel, uri: String?): Color? {
    val sizePx = with(LocalDensity.current) { TintSampleSize.roundToPx() }
    return rememberArtworkColor(rememberArtwork(viewModel, uri, sizePx))
}

private val TintSampleSize = 48.dp
