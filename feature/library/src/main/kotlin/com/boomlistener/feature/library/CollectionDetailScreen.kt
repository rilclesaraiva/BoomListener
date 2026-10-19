// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.core.common.ExternalLinks
import com.boomlistener.core.common.openUrl
import com.boomlistener.core.common.shareLink
import com.boomlistener.core.designsystem.component.BoomArtwork
import com.boomlistener.core.designsystem.component.BoomFastScroller
import com.boomlistener.core.designsystem.component.boomIndexLabel
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.designsystem.component.BoomListArtworkSize
import com.boomlistener.core.designsystem.component.BoomLoading
import com.boomlistener.core.designsystem.component.BoomMenuAction
import com.boomlistener.core.designsystem.component.BoomMoreMenu
import com.boomlistener.core.designsystem.component.BoomPlaceholder
import com.boomlistener.core.designsystem.component.BoomScreenScaffold
import com.boomlistener.core.designsystem.component.clickableWithMenu
import com.boomlistener.core.model.Track
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private val HeaderCover = 168.dp

/**
 * Um gênero, álbum ou playlist aberto: capa, Tocar / Aleatório e as músicas.
 * Na playlist, segurar o puxador reordena e o menu tira a música.
 */
@Composable
internal fun CollectionDetailScreen(
    viewModel: CollectionDetailViewModel,
    onBack: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onEditTags: (Track) -> Unit,
    onRecommend: (Track) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ready = state as? CollectionDetailUiState.Ready
    var renaming by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var addToPlaylist by remember { mutableStateOf<Track?>(null) }
    var addingSongs by remember { mutableStateOf(false) }
    val deleteSong = rememberSongDeleter()

    BoomScreenScaffold(
        title = ready?.name.orEmpty(),
        onBack = onBack,
        modifier = modifier,
        actions = {
            if (ready?.isPlaylist == true) {
                BoomMoreMenu(
                    actions = listOf(
                        BoomMenuAction(stringResource(R.string.feature_library_add_songs)) { addingSongs = true },
                        BoomMenuAction(stringResource(R.string.feature_library_rename_playlist)) { renaming = true },
                        BoomMenuAction(stringResource(R.string.feature_library_delete_playlist)) { confirmDelete = true },
                    ),
                )
            }
        },
    ) { padding ->
        when (val current = state) {
            CollectionDetailUiState.Loading -> BoomLoading(Modifier.padding(padding))
            CollectionDetailUiState.Missing -> BoomPlaceholder(
                title = stringResource(R.string.feature_library_collection_missing_title),
                message = stringResource(R.string.feature_library_collection_missing_message),
                modifier = Modifier.padding(padding),
            )
            is CollectionDetailUiState.Ready -> TrackListWithHeader(
                state = current,
                viewModel = viewModel,
                padding = padding,
                onOpenArtist = onOpenArtist,
                onEditTags = onEditTags,
                onRecommend = onRecommend,
                onAddToPlaylist = { addToPlaylist = it },
                onDelete = deleteSong,
                onAddSongs = { addingSongs = true },
            )
        }
    }

    if (addingSongs && ready?.isPlaylist == true) {
        AddSongsSheet(viewModel = viewModel, onDismiss = { addingSongs = false })
    }

    addToPlaylist?.let { track ->
        AddToPlaylistSheet(trackUris = listOf(track.uri), onDismiss = { addToPlaylist = null })
    }
    if (renaming && ready != null) {
        PlaylistNameDialog(
            title = stringResource(R.string.feature_library_rename_playlist),
            initialName = ready.name,
            confirmLabel = stringResource(R.string.feature_library_save),
            onConfirm = { name ->
                renaming = false
                viewModel.rename(name)
            },
            onDismiss = { renaming = false },
        )
    }
    if (confirmDelete && ready != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.feature_library_delete_playlist_title, ready.name)) },
            text = { Text(stringResource(R.string.feature_library_delete_playlist_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete()
                    onBack()
                }) { Text(stringResource(R.string.feature_library_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.feature_library_cancel)) }
            },
        )
    }
}

@Composable
private fun TrackListWithHeader(
    state: CollectionDetailUiState.Ready,
    viewModel: CollectionDetailViewModel,
    padding: androidx.compose.foundation.layout.PaddingValues,
    onOpenArtist: (String) -> Unit,
    onEditTags: (Track) -> Unit,
    onRecommend: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onDelete: (Track) -> Unit,
    onAddSongs: () -> Unit,
) {
    val listState = rememberLazyListState()
    // Reordenar mexe na lista da tela na hora; a playlist só é gravada ao soltar.
    var local by remember { mutableStateOf(state.tracks) }
    var dragging by remember { mutableStateOf(false) }
    var dragFromUri by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state.tracks) { if (!dragging) local = state.tracks }
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIndex = local.indexOfFirst { it.uri == from.key }
        val toIndex = local.indexOfFirst { it.uri == to.key }
        if (fromIndex >= 0 && toIndex >= 0) {
            local = local.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        }
    }

    Box(Modifier.fillMaxSize()) {
    LazyColumn(state = listState, contentPadding = padding) {
        item(key = "header") { Header(state, viewModel, onAddSongs) }
        if (state.tracks.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.feature_library_playlist_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }
        items(local, key = { it.uri }) { track ->
            ReorderableItem(reorderState, key = track.uri, enabled = state.isPlaylist) { isDragging ->
                Surface(shadowElevation = if (isDragging) 8.dp else 0.dp) {
                    DetailTrackRow(
                        track = track,
                        viewModel = viewModel,
                        isPlaylist = state.isPlaylist,
                        onClick = { viewModel.play(local.indexOf(track)) },
                        onOpenArtist = onOpenArtist,
                        onEditTags = onEditTags,
                        onRecommend = onRecommend,
                        onAddToPlaylist = onAddToPlaylist,
                        onDelete = { onDelete(track) },
                        dragHandle = {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .longPressDraggableHandle(
                                        onDragStarted = {
                                            dragging = true
                                            dragFromUri = track.uri
                                        },
                                        onDragStopped = {
                                            dragging = false
                                            val from = dragFromUri
                                            val newIndex = local.indexOfFirst { it.uri == track.uri }
                                            // A música que estava no lugar novo, na ordem gravada.
                                            val target = state.tracks.getOrNull(newIndex)?.uri
                                            if (from != null && target != null && from != target) viewModel.move(from, target)
                                        },
                                    ),
                            ) {
                                Icon(BoomIcons.DragHandle, stringResource(R.string.feature_library_reorder))
                            }
                        },
                    )
                }
            }
        }
    }
    // Arrastar pela direita pula pela lista, mostrando a letra.
    BoomFastScroller(
        state = listState,
        label = { index -> boomIndexLabel(local.getOrNull((index - 1).coerceAtLeast(0))?.title) },
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding()),
    )
    }
}

@Composable
private fun Header(state: CollectionDetailUiState.Ready, viewModel: CollectionDetailViewModel, onAddSongs: () -> Unit) {
    val px = with(LocalDensity.current) { HeaderCover.roundToPx() }
    val coverUri = state.tracks.firstOrNull()?.uri
    val cover by produceState<ImageBitmap?>(initialValue = null, coverUri) {
        value = coverUri?.let { viewModel.artwork(it, px)?.asImageBitmap() }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
    ) {
        BoomArtwork(image = cover, fallbackText = state.name, size = HeaderCover)
        Spacer(Modifier.size(12.dp))
        Text(state.name, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(
            text = listOfNotNull(
                state.subtitle,
                state.year?.toString(),
                pluralStringResource(R.plurals.feature_library_count, state.tracks.size, state.tracks.size),
                // Duração do álbum (ou playlist) inteiro: "1 h 05 min".
                state.tracks.takeIf { it.isNotEmpty() }?.let { tracks -> totalDuration(tracks.sumOf { it.durationMs }) },
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { viewModel.play() }, enabled = state.tracks.isNotEmpty()) {
                Icon(BoomIcons.Play, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.feature_library_play))
            }
            FilledTonalButton(onClick = viewModel::shuffle, enabled = state.tracks.isNotEmpty()) {
                Icon(BoomIcons.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.feature_library_shuffle_all))
            }
        }
        if (state.isPlaylist) {
            Spacer(Modifier.size(8.dp))
            OutlinedButton(onClick = onAddSongs) {
                Icon(BoomIcons.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.feature_library_add_songs))
            }
        }
    }
}

@Composable
private fun DetailTrackRow(
    track: Track,
    viewModel: CollectionDetailViewModel,
    isPlaylist: Boolean,
    onClick: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onEditTags: (Track) -> Unit,
    onRecommend: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onDelete: () -> Unit,
    dragHandle: @Composable () -> Unit,
) {
    val menuOpen = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val px = with(LocalDensity.current) { BoomListArtworkSize.roundToPx() }
    val artwork by produceState<ImageBitmap?>(initialValue = null, track.uri) {
        value = viewModel.artwork(track.uri, px)?.asImageBitmap()
    }
    ListItem(
        // Segurar a música abre o mesmo menu dos três pontinhos.
        modifier = Modifier.clickableWithMenu(menuOpen, onClick),
        leadingContent = {
            // Na playlist, o puxador de reordenar fica ao lado da capa.
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isPlaylist) dragHandle()
                BoomArtwork(image = artwork, fallbackText = track.title)
            }
        },
        headlineContent = { Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                text = listOfNotNull(track.artist ?: stringResource(R.string.feature_library_unknown_artist), track.album)
                    .joinToString(" • "),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
                BoomMoreMenu(
                    actions = listOfNotNull(
                        BoomMenuAction(stringResource(R.string.feature_library_add_to_playlist)) { onAddToPlaylist(track) },
                        BoomMenuAction(stringResource(R.string.feature_library_recommend)) { onRecommend(track) },
                        if (isPlaylist) {
                            BoomMenuAction(stringResource(R.string.feature_library_remove_from_playlist)) { viewModel.remove(track) }
                        } else {
                            null
                        },
                        track.artist?.let { name ->
                            BoomMenuAction(stringResource(R.string.feature_library_go_to_artist)) { onOpenArtist(name) }
                        },
                        BoomMenuAction(stringResource(R.string.feature_library_edit_tags)) { onEditTags(track) },
                        BoomMenuAction(stringResource(R.string.feature_library_delete_song), onClick = onDelete),
                    ),
                    onShare = {
                        scope.launch {
                            context.shareLink(
                                url = viewModel.shareUrl(track),
                                title = listOfNotNull(track.title, track.artist).joinToString(" — "),
                            )
                        }
                    },
                    onYoutubeMusic = { context.openUrl(ExternalLinks.youtubeMusicSearch(track.title, track.artist)) },
                    openState = menuOpen,
                )
        },
    )
}

/** "1 h 05 min" ou "42 min". */
@Composable
private fun totalDuration(totalMs: Long): String {
    val (hours, minutes) = hoursAndMinutes(totalMs)
    return if (hours > 0) {
        stringResource(R.string.feature_library_total_hours, hours.toInt(), minutes.toInt())
    } else {
        stringResource(R.string.feature_library_total_minutes, minutes.toInt())
    }
}
