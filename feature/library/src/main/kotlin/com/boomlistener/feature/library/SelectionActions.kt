// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.designsystem.component.BoomMenuAction
import com.boomlistener.core.designsystem.component.BoomMenuKind
import com.boomlistener.core.model.Track
import com.boomlistener.core.player.PlayerConnection
import com.boomlistener.data.library.FavoritesData
import kotlinx.coroutines.launch

/**
 * Quais músicas o "favoritar" da seleção mexe: se todas já são favoritas, tira
 * todas; senão, favorita só as que ainda não são (nenhuma é desfavoritada sem
 * querer).
 */
internal fun favoriteTargets(selected: List<String>, favorites: Collection<String>): List<String> {
    val allFavorite = selected.isNotEmpty() && selected.all { it in favorites }
    return if (allFavorite) selected else selected.filter { it !in favorites }
}

/**
 * As ações do ⋮ da barra de seleção, sobre as [tracks] marcadas (na ordem da
 * lista). [onRemoveFromPlaylist] só existe dentro de uma playlist.
 */
@Composable
internal fun selectionActions(
    tracks: List<Track>,
    onEditTags: (List<String>) -> Unit,
    onAddToPlaylist: (List<Track>) -> Unit,
    onDelete: (List<Track>) -> Unit,
    onRemoveFromPlaylist: ((List<Track>) -> Unit)? = null,
): List<BoomMenuAction> {
    val context = LocalContext.current.applicationContext
    val controller = remember { PlayerConnection.controller(context) }
    val favorites = remember { FavoritesData.repository(context) }
    val favoriteUris by favorites.favorites.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val queued = stringResource(R.string.feature_library_play_next_done)
    val allFavorite = tracks.isNotEmpty() && tracks.all { it.uri in favoriteUris }
    return listOfNotNull(
        BoomMenuAction(stringResource(R.string.feature_library_edit_tags), kind = BoomMenuKind.EditTags) {
            onEditTags(tracks.map { it.uri })
        },
        BoomMenuAction(stringResource(R.string.feature_library_play_next), kind = BoomMenuKind.PlayNext) {
            // Do fim para o começo: cada uma entra logo depois da atual, então ficam na ordem da lista.
            tracks.asReversed().forEach(controller::playNext)
            Toast.makeText(context, queued, Toast.LENGTH_SHORT).show()
        },
        BoomMenuAction(stringResource(R.string.feature_library_add_to_playlist), kind = BoomMenuKind.AddToPlaylist) {
            onAddToPlaylist(tracks)
        },
        BoomMenuAction(
            stringResource(if (allFavorite) R.string.feature_library_unfavorite else R.string.feature_library_favorite),
            kind = BoomMenuKind.Favorite,
            icon = if (allFavorite) BoomIcons.Favorite else BoomIcons.FavoriteBorder,
        ) {
            val targets = favoriteTargets(tracks.map { it.uri }, favoriteUris)
            scope.launch { targets.forEach { favorites.toggle(it) } }
        },
        onRemoveFromPlaylist?.let { remove ->
            BoomMenuAction(
                stringResource(R.string.feature_library_remove_from_playlist),
                kind = BoomMenuKind.RemoveFromPlaylist,
            ) { remove(tracks) }
        },
        BoomMenuAction(stringResource(R.string.feature_library_delete_song), kind = BoomMenuKind.Delete) { onDelete(tracks) },
    )
}
