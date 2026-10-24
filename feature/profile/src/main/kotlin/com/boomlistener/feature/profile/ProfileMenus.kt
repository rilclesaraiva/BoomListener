// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.core.common.ExternalLinks
import com.boomlistener.core.common.openUrl
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.designsystem.component.BoomMenuAction
import com.boomlistener.core.designsystem.component.BoomMenuHeader
import com.boomlistener.core.designsystem.component.BoomMenuKind
import com.boomlistener.core.designsystem.component.BoomMenuSheet
import com.boomlistener.data.library.FavoritesData
import kotlinx.coroutines.launch

/** O que os menus do perfil levam para fora dele (o :app liga). */
class ProfileSongMenu(
    val onOpenArtist: (String) -> Unit,
    val onEditTags: (uri: String) -> Unit,
    val onAddToPlaylist: (uri: String) -> Unit,
)

/** "Favoritar" / "Tirar dos favoritos" no menu de uma música. */
@Composable
private fun favoriteAction(uri: String): BoomMenuAction {
    val context = LocalContext.current.applicationContext
    val repository = remember { FavoritesData.repository(context) }
    val favorites by repository.favorites.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val favorite = uri in favorites
    return BoomMenuAction(
        stringResource(if (favorite) R.string.feature_profile_unfavorite else R.string.feature_profile_favorite),
        kind = BoomMenuKind.Favorite,
        icon = if (favorite) BoomIcons.Favorite else BoomIcons.FavoriteBorder,
    ) { scope.launch { repository.toggle(uri) } }
}

/** Menu de uma música do perfil (mais ouvidas e histórico), aberto ao segurar. */
@Composable
internal fun ProfileSongMenuSheet(
    open: MutableState<Boolean>,
    uri: String,
    title: String,
    artist: String?,
    cover: ImageBitmap?,
    menu: ProfileSongMenu,
    onPlay: () -> Unit,
) {
    val context = LocalContext.current
    BoomMenuSheet(
        openState = open,
        actions = listOfNotNull(
            BoomMenuAction(stringResource(R.string.feature_profile_play), kind = BoomMenuKind.PlayNow, onClick = onPlay),
            favoriteAction(uri),
            playNextAction(uri),
            BoomMenuAction(stringResource(R.string.feature_profile_add_to_playlist), kind = BoomMenuKind.AddToPlaylist) { menu.onAddToPlaylist(uri) },
            artist?.takeIf { it.isNotBlank() }?.let { name ->
                BoomMenuAction(stringResource(R.string.feature_profile_go_to_artist), kind = BoomMenuKind.GoToArtist) {
                    menu.onOpenArtist(artistOf(name))
                }
            },
            BoomMenuAction(stringResource(R.string.feature_profile_edit_tags), kind = BoomMenuKind.EditTags) { menu.onEditTags(uri) },
        ),
        onYoutubeMusic = { context.openUrl(ExternalLinks.youtubeMusicSearch(title, artist)) },
        header = BoomMenuHeader(title, artist, cover),
    )
}

/** Menu de um artista do perfil: tocar as músicas dele, aleatório, abrir a página. */
@Composable
internal fun ProfileArtistMenuSheet(
    open: MutableState<Boolean>,
    name: String,
    photo: ImageBitmap?,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onOpenArtist: () -> Unit,
) {
    BoomMenuSheet(
        openState = open,
        actions = listOf(
            BoomMenuAction(stringResource(R.string.feature_profile_play), kind = BoomMenuKind.PlayNow, onClick = onPlay),
            BoomMenuAction(stringResource(R.string.feature_profile_shuffle), kind = BoomMenuKind.Mix, onClick = onShuffle),
            BoomMenuAction(stringResource(R.string.feature_profile_go_to_artist), kind = BoomMenuKind.GoToArtist, onClick = onOpenArtist),
        ),
        header = BoomMenuHeader(name, image = photo, round = true),
    )
}
