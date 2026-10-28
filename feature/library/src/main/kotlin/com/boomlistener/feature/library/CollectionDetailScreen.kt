// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
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
import com.boomlistener.core.designsystem.component.BoomMosaicArtwork
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
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
import com.boomlistener.core.designsystem.component.BoomMenuHeader
import com.boomlistener.core.designsystem.component.BoomMenuKind
import com.boomlistener.core.designsystem.component.BoomMoreMenu
import com.boomlistener.core.designsystem.component.BoomPlaceholder
import com.boomlistener.core.designsystem.component.BoomScreenScaffold
import com.boomlistener.core.designsystem.component.clickableWithMenu
import com.boomlistener.core.designsystem.component.trackPress
import com.boomlistener.core.model.Track
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/** Altura máxima da capa do topo (em pé ela é quadrada, da largura da tela). */
private val HeaderMaxHeight = 420.dp

/** Tamanho em que as capas do topo são carregadas. */
private val HeaderImageSize = 420.dp

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
    modifier: Modifier = Modifier,
    /** Abre o álbum de uma música (null dentro do próprio álbum). */
    onOpenAlbum: ((albumKey: String) -> Unit)? = null,
) {
    val rawState by viewModel.state.collectAsStateWithLifecycle()
    // Favoritas: o nome vem do texto da tela (no idioma do celular).
    val favoritesName = stringResource(R.string.feature_library_favorites)
    val state = (rawState as? CollectionDetailUiState.Ready)?.takeIf { it.isFavorites }?.copy(name = favoritesName) ?: rawState
    val ready = state as? CollectionDetailUiState.Ready
    var renaming by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var addToPlaylist by remember { mutableStateOf<Track?>(null) }
    var addingSongs by remember { mutableStateOf(false) }
    val deleteSong = rememberSongDeleter()
    val showDetails = rememberSongDetails()
    // Capa do topo do menu da playlist: a da primeira música.
    val headerPx = with(LocalDensity.current) { 56.dp.roundToPx() }
    val firstUri = ready?.tracks?.firstOrNull()?.uri
    val playlistCover by produceState<ImageBitmap?>(initialValue = null, firstUri) {
        value = firstUri?.let { viewModel.artwork(it, headerPx)?.asImageBitmap() }
    }

    // Menu da playlist (adicionar, renomear, apagar): fica na linha de botões do topo.
    val playlistMenu: @Composable () -> Unit = {
            if (ready?.isPlaylist == true) {
                BoomMoreMenu(
                    actions = listOf(
                        BoomMenuAction(stringResource(R.string.feature_library_add_songs), kind = BoomMenuKind.AddToPlaylist) { addingSongs = true },
                        BoomMenuAction(stringResource(R.string.feature_library_rename_playlist), kind = BoomMenuKind.Rename) { renaming = true },
                        BoomMenuAction(stringResource(R.string.feature_library_delete_playlist), kind = BoomMenuKind.Delete) { confirmDelete = true },
                    ),
                    header = BoomMenuHeader(
                        title = ready.name,
                        subtitle = pluralStringResource(R.plurals.feature_library_count, ready.tracks.size, ready.tracks.size),
                        image = playlistCover,
                    ),
                )
            }
    }

    BoomScreenScaffold(
        title = ready?.name.orEmpty(),
        onBack = onBack,
        modifier = modifier,
        // Sem barra presa: a capa grande é o topo da página (o voltar fica parado no canto).
        topBar = {},
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
                onAddToPlaylist = { addToPlaylist = it },
                onDelete = deleteSong,
                onDetails = { showDetails(it.uri, it.title) },
                onAddSongs = { addingSongs = true },
                onOpenAlbum = onOpenAlbum,
                playlistMenu = playlistMenu,
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
    onAddToPlaylist: (Track) -> Unit,
    onDelete: (Track) -> Unit,
    onDetails: (Track) -> Unit,
    onAddSongs: () -> Unit,
    onOpenAlbum: ((String) -> Unit)?,
    playlistMenu: @Composable () -> Unit,
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
        item(key = "header") { Header(state, viewModel, onAddSongs, playlistMenu) }
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
                        onAddToPlaylist = onAddToPlaylist,
                        onDelete = { onDelete(track) },
                        onDetails = { onDetails(track) },
                        onOpenAlbum = onOpenAlbum,
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

/**
 * Topo de playlist, álbum ou gênero: a capa grande de ponta a ponta (montada
 * com quatro capas nas playlists, [mosaicCovers]) esmaecendo no fundo, o nome
 * centralizado em cima dela e a linha de botões (tocar redondo e grande à
 * direita). Diferente da página do artista, que tem foto, capa redonda e o
 * nome à esquerda, para não se confundirem.
 */
@Composable
private fun Header(
    state: CollectionDetailUiState.Ready,
    viewModel: CollectionDetailViewModel,
    onAddSongs: () -> Unit,
    playlistMenu: @Composable () -> Unit,
) {
    val px = with(LocalDensity.current) { HeaderImageSize.roundToPx() }
    val covers = if (state.isPlaylist || state.isFavorites) mosaicCovers(state.tracks) else listOfNotNull(state.tracks.firstOrNull())
    val uris = covers.map { it.uri }
    val images by produceState<List<ImageBitmap?>>(initialValue = List(uris.size) { null }, uris) {
        value = uris.map { uri -> viewModel.artwork(uri, px)?.asImageBitmap() }
    }
    val background = MaterialTheme.colorScheme.background
    val hasTracks = state.tracks.isNotEmpty()

    Column(Modifier.fillMaxWidth()) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().height(minOf(maxWidth, HeaderMaxHeight))) {
                when {
                    covers.size >= MOSAIC_SIZE -> BoomMosaicArtwork(images, size = null, shape = RectangleShape, modifier = Modifier.fillMaxSize())
                    images.firstOrNull() != null -> Image(
                        bitmap = images.first()!!,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    else -> Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.secondaryContainer),
                    ) {
                        Icon(
                            if (state.isPlaylist || state.isFavorites) BoomIcons.Playlist else BoomIcons.Album,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(72.dp),
                        )
                    }
                }
                // A capa esmaece até o fundo da página; o nome fica em cima dessa parte.
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to background)))
                Text(
                    state.name,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 24.dp),
                )
            }
        }
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
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp),
        )
        // Ações de apoio à esquerda; aleatório e tocar (o maior) à direita.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
        ) {
            if (state.isPlaylist) {
                FilledTonalIconButton(onClick = onAddSongs, modifier = Modifier.size(48.dp)) {
                    Icon(BoomIcons.Add, contentDescription = stringResource(R.string.feature_library_add_songs))
                }
                playlistMenu()
            }
            Spacer(Modifier.weight(1f))
            FilledTonalIconButton(onClick = viewModel::shuffle, enabled = hasTracks, modifier = Modifier.size(48.dp)) {
                Icon(BoomIcons.Shuffle, contentDescription = stringResource(R.string.feature_library_shuffle_all))
            }
            FilledIconButton(
                onClick = { viewModel.play() },
                enabled = hasTracks,
                shape = CircleShape,
                modifier = Modifier.size(64.dp),
            ) {
                Icon(BoomIcons.Play, contentDescription = stringResource(R.string.feature_library_play), modifier = Modifier.size(32.dp))
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
    onAddToPlaylist: (Track) -> Unit,
    onDelete: () -> Unit,
    onDetails: () -> Unit,
    onOpenAlbum: ((String) -> Unit)?,
    dragHandle: @Composable () -> Unit,
) {
    val menuOpen = remember { mutableStateOf(false) }
    // Dedo no puxador = arrastar; aí segurar não abre o menu.
    val handlePressed = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val px = with(LocalDensity.current) { BoomListArtworkSize.roundToPx() }
    val artwork by produceState<ImageBitmap?>(initialValue = null, track.uri) {
        value = viewModel.artwork(track.uri, px)?.asImageBitmap()
    }
    ListItem(
        // Segurar a música abre o mesmo menu dos três pontinhos.
        modifier = Modifier.clickableWithMenu(menuOpen, longPressEnabled = { !handlePressed.value }, onClick = onClick),
        leadingContent = {
            // Na playlist, o puxador de reordenar fica ao lado da capa.
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isPlaylist) Box(Modifier.trackPress(handlePressed)) { dragHandle() }
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
                        playNextAction(track),
                        BoomMenuAction(stringResource(R.string.feature_library_add_to_playlist), kind = BoomMenuKind.AddToPlaylist) { onAddToPlaylist(track) },
                        favoriteAction(track.uri),
                        if (isPlaylist) {
                            BoomMenuAction(
                                stringResource(R.string.feature_library_remove_from_playlist),
                                kind = BoomMenuKind.RemoveFromPlaylist,
                            ) { viewModel.remove(track) }
                        } else {
                            null
                        },
                        onOpenAlbum?.takeIf { !track.album.isNullOrBlank() }?.let { open ->
                            BoomMenuAction(stringResource(R.string.feature_library_go_to_album), kind = BoomMenuKind.GoToAlbum) {
                                open(track.albumId.toString())
                            }
                        },
                        track.artist?.let { name ->
                            BoomMenuAction(stringResource(R.string.feature_library_go_to_artist), kind = BoomMenuKind.GoToArtist) { onOpenArtist(name) }
                        },
                        BoomMenuAction(stringResource(R.string.feature_library_edit_tags), kind = BoomMenuKind.EditTags) { onEditTags(track) },
                        BoomMenuAction(stringResource(R.string.feature_library_details), kind = BoomMenuKind.Details, onClick = onDetails),
                        BoomMenuAction(stringResource(R.string.feature_library_delete_song), kind = BoomMenuKind.Delete, onClick = onDelete),
                    ),
                    header = BoomMenuHeader(track.title, track.artist, artwork),
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
