// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import androidx.compose.runtime.Composable
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
        )
    }
}

/** Capa da música [uri] em ~[sizePx], carregada fora da thread da tela. */
@Composable
internal fun rememberArtwork(viewModel: NowPlayingViewModel, uri: String?, sizePx: Int): ImageBitmap? {
    // Muda quando alguma tag foi editada: a capa é lida de novo.
    val version by viewModel.libraryVersion.collectAsStateWithLifecycle()
    val artwork by produceState<ImageBitmap?>(initialValue = null, uri, sizePx, version) {
        value = uri?.let { viewModel.artwork(it, sizePx)?.asImageBitmap() }
    }
    return artwork
}
