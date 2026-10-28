// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.boomlistener.core.designsystem.component.BoomArtistChooserSheet
import com.boomlistener.data.deezer.DeezerData
import com.boomlistener.data.deezer.artistPicture
import com.boomlistener.data.deezer.splitArtistNames

/**
 * Abrir o artista de uma música com vários ("Lilbubblegum, 1nonly", "A feat.
 * B"): em vez de abrir uma página com o nome junto, sobe um painel com cada
 * artista separado, com a foto (`BoomArtistChooserSheet`). Com um artista
 * só, abre direto.
 *
 * Público porque o `:app` também usa nos menus das outras telas (Início,
 * Biblioteca, Perfil, Histórico).
 */
@Composable
fun rememberArtistOpener(onOpenArtist: (String) -> Unit): (String) -> Unit {
    val context = LocalContext.current.applicationContext
    val deezer = remember { DeezerData.repository(context) }
    var choosing by remember { mutableStateOf<String?>(null) }
    choosing?.let { full ->
        BoomArtistChooserSheet(
            fullName = full,
            artists = splitArtistNames(full),
            photo = { name, sizePx -> deezer.artistPicture(name, sizePx)?.asImageBitmap() },
            onPick = { name ->
                choosing = null
                onOpenArtist(name)
            },
            onDismiss = { choosing = null },
        )
    }
    return remember(onOpenArtist) {
        { name -> if (splitArtistNames(name).size > 1) choosing = name else onOpenArtist(name) }
    }
}
