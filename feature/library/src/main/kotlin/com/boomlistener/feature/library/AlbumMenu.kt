// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.boomlistener.core.common.ExternalLinks
import com.boomlistener.core.common.openUrl
import com.boomlistener.core.designsystem.component.BoomMenuAction
import com.boomlistener.core.designsystem.component.BoomMenuHeader
import com.boomlistener.core.designsystem.component.BoomMenuKind
import com.boomlistener.core.designsystem.component.BoomMenuSheet
import com.boomlistener.core.model.Track

/** O que o menu de um álbum (segurar o bloco) pode fazer. */
internal class AlbumMenuActions(
    val play: (List<Track>) -> Unit,
    val shuffle: (List<Track>) -> Unit,
    val addToPlaylist: (List<Track>) -> Unit,
    val openArtist: (String) -> Unit,
)

/**
 * Menu de um álbum, aberto ao segurar o bloco (Início e Biblioteca → Álbuns):
 * tocar, aleatório, adicionar à playlist, ir para o artista e ouvir no
 * YouTube Music.
 */
@Composable
internal fun AlbumMenuSheet(
    open: MutableState<Boolean>,
    name: String,
    artist: String?,
    tracks: List<Track>,
    cover: ImageBitmap?,
    actions: AlbumMenuActions,
) {
    val context = LocalContext.current
    BoomMenuSheet(
        openState = open,
        actions = listOfNotNull(
            BoomMenuAction(stringResource(R.string.feature_library_play), kind = BoomMenuKind.PlayNow) { actions.play(tracks) },
            BoomMenuAction(stringResource(R.string.feature_library_shuffle_all), kind = BoomMenuKind.Mix) { actions.shuffle(tracks) },
            BoomMenuAction(stringResource(R.string.feature_library_add_to_playlist), kind = BoomMenuKind.AddToPlaylist) {
                actions.addToPlaylist(tracks)
            },
            artist?.takeIf { it.isNotBlank() }?.let { name ->
                BoomMenuAction(stringResource(R.string.feature_library_go_to_artist), kind = BoomMenuKind.GoToArtist) { actions.openArtist(name) }
            },
        ),
        onYoutubeMusic = { context.openUrl(ExternalLinks.youtubeMusicSearch(name, artist)) },
        header = BoomMenuHeader(name, artist, cover),
    )
}
