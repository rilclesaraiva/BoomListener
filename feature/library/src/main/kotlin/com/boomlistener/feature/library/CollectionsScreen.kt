// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.core.designsystem.component.BoomArtwork
import com.boomlistener.core.designsystem.component.BoomCarousel
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.designsystem.component.BoomListArtworkSize
import com.boomlistener.core.designsystem.component.BoomLoading
import com.boomlistener.core.designsystem.component.BoomScreenScaffold
import com.boomlistener.core.designsystem.component.BoomTopActions
import com.boomlistener.core.designsystem.theme.boomEffectsSpec
import com.boomlistener.core.model.Track

private val CardHeight = 176.dp

/**
 * Aba Biblioteca: um carrossel no topo com Gêneros, Artistas, Playlists e
 * Álbuns; tocar num card mostra aquela seção embaixo.
 */
@Composable
internal fun CollectionsScreen(
    viewModel: CollectionsViewModel,
    onOpenGenre: (key: String) -> Unit,
    onOpenArtist: (name: String) -> Unit,
    onOpenAlbum: (key: String) -> Unit,
    onOpenPlaylist: (id: Long) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }

    BoomScreenScaffold(
        title = stringResource(R.string.feature_library_collections_title),
        modifier = modifier,
        actions = { BoomTopActions(onOpenHistory = onOpenHistory, onOpenSettings = onOpenSettings) },
    ) { padding ->
        when (val current = state) {
            CollectionsUiState.Loading -> BoomLoading(Modifier.padding(padding))
            is CollectionsUiState.Ready -> LazyColumn(contentPadding = padding) {
                item(key = "carousel") {
                    CategoryCarousel(current, viewModel, onSelect = viewModel::select)
                    Spacer(Modifier.height(8.dp))
                }
                when (current.selected) {
                    CollectionKind.Genres -> {
                        if (current.genres.isEmpty()) item(key = "empty") { EmptySection(R.string.feature_library_genres_empty) }
                        items(current.genres, key = { "g:" + it.key }) { group ->
                            GroupRow(
                                name = group.name,
                                count = group.tracks.size,
                                cover = group.tracks.firstOrNull(),
                                viewModel = viewModel,
                                onClick = { onOpenGenre(group.key) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                    CollectionKind.Artists -> {
                        if (current.artists.isEmpty()) item(key = "empty") { EmptySection(R.string.feature_library_artists_empty) }
                        items(current.artists, key = { "a:" + it.key }) { group ->
                            GroupRow(
                                name = group.name,
                                count = group.tracks.size,
                                cover = group.tracks.firstOrNull(),
                                viewModel = viewModel,
                                round = true,
                                onClick = { onOpenArtist(group.name) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                    CollectionKind.Playlists -> {
                        item(key = "new") {
                            FilledTonalButton(
                                onClick = { creating = true },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).animateItem(),
                            ) {
                                Icon(BoomIcons.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(8.dp))
                                Text(stringResource(R.string.feature_library_new_playlist))
                            }
                        }
                        if (current.playlists.isEmpty()) item(key = "empty") { EmptySection(R.string.feature_library_playlists_empty) }
                        items(current.playlists, key = { "p:" + it.playlist.id }) { summary ->
                            GroupRow(
                                name = summary.playlist.name,
                                count = summary.tracks.size,
                                cover = summary.tracks.firstOrNull(),
                                viewModel = viewModel,
                                fallbackIcon = BoomIcons.Playlist,
                                onClick = { onOpenPlaylist(summary.playlist.id) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                    CollectionKind.Albums -> {
                        if (current.albums.isEmpty()) item(key = "empty") { EmptySection(R.string.feature_library_albums_empty) }
                        // Grade de duas colunas dentro da lista.
                        items(current.albums.chunked(2), key = { "al:" + it.first().key }) { pair ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp).animateItem(),
                            ) {
                                pair.forEach { album ->
                                    AlbumTile(
                                        name = album.name,
                                        artist = listOfNotNull(album.subtitle, album.year?.toString()).joinToString(" · "),
                                        cover = album.tracks.firstOrNull(),
                                        viewModel = viewModel,
                                        onClick = { onOpenAlbum(album.key) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }

    if (creating) {
        val count = (state as? CollectionsUiState.Ready)?.playlists?.size ?: 0
        PlaylistNameDialog(
            title = stringResource(R.string.feature_library_new_playlist),
            initialName = stringResource(R.string.feature_library_playlist_default_name, count + 1),
            confirmLabel = stringResource(R.string.feature_library_create),
            onConfirm = { name ->
                creating = false
                viewModel.createPlaylist(name)
            },
            onDismiss = { creating = false },
        )
    }
}

/** Os quatro cards do topo, cada um com a capa de algo daquela seção. */
@Composable
private fun CategoryCarousel(
    state: CollectionsUiState.Ready,
    viewModel: CollectionsViewModel,
    onSelect: (CollectionKind) -> Unit,
) {
    val kinds = CollectionKind.entries
    BoomCarousel(
        itemCount = kinds.size,
        modifier = Modifier.fillMaxWidth().height(CardHeight),
    ) { index, itemModifier ->
        val kind = kinds[index]
        val (label, icon, count, cover) = when (kind) {
            CollectionKind.Genres -> CardInfo(R.string.feature_library_genres, BoomIcons.Genre, state.genres.size, state.genres.firstOrNull()?.tracks?.firstOrNull())
            CollectionKind.Artists -> CardInfo(R.string.feature_library_artists, BoomIcons.Person, state.artists.size, state.artists.firstOrNull()?.tracks?.firstOrNull())
            CollectionKind.Playlists -> CardInfo(R.string.feature_library_playlists, BoomIcons.Playlist, state.playlists.size, state.playlists.firstOrNull()?.tracks?.firstOrNull())
            CollectionKind.Albums -> CardInfo(R.string.feature_library_albums, BoomIcons.Album, state.albums.size, state.albums.firstOrNull()?.tracks?.firstOrNull())
        }
        CategoryCard(
            label = stringResource(label),
            icon = icon,
            count = count,
            cover = cover,
            selected = state.selected == kind,
            viewModel = viewModel,
            onClick = { onSelect(kind) },
            modifier = itemModifier,
        )
    }
}

private data class CardInfo(val label: Int, val icon: ImageVector, val count: Int, val cover: Track?)

@Composable
private fun CategoryCard(
    label: String,
    icon: ImageVector,
    count: Int,
    cover: Track?,
    selected: Boolean,
    viewModel: CollectionsViewModel,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val px = with(LocalDensity.current) { 240.dp.roundToPx() }
    val image by produceState<ImageBitmap?>(initialValue = null, cover?.uri) {
        value = cover?.let { viewModel.artwork(it.uri, px)?.asImageBitmap() }
    }
    // O card escolhido fica na cor de destaque; os outros, no tom neutro.
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = boomEffectsSpec(),
        label = "cardColor",
    )
    val content = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Box(
        modifier
            .fillMaxSize()
            .background(container)
            .clickable(onClick = onClick),
    ) {
        image?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.55f,
                modifier = Modifier.fillMaxSize(),
            )
            // Escurece de baixo para cima para o nome ficar legível sobre a capa.
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, container))))
        }
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = content)
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.titleLarge, color = content, maxLines = 1)
            Text(count.toString(), style = MaterialTheme.typography.labelLarge, color = content)
        }
    }
}

@Composable
private fun GroupRow(
    name: String,
    count: Int,
    cover: Track?,
    viewModel: CollectionsViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    round: Boolean = false,
    fallbackIcon: ImageVector? = null,
) {
    val px = with(LocalDensity.current) { BoomListArtworkSize.roundToPx() }
    val image by produceState<ImageBitmap?>(initialValue = null, cover?.uri) {
        value = cover?.let { viewModel.artwork(it.uri, px)?.asImageBitmap() }
    }
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        leadingContent = {
            if (image == null && fallbackIcon != null) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.size(BoomListArtworkSize),
                ) {
                    Box(contentAlignment = Alignment.Center) { Icon(fallbackIcon, contentDescription = null) }
                }
            } else {
                BoomArtwork(
                    image = image,
                    fallbackText = name,
                    modifier = if (round) Modifier.clip(CircleShape) else Modifier,
                )
            }
        },
        headlineContent = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(pluralStringResource(R.plurals.feature_library_count, count, count)) },
    )
}

@Composable
private fun AlbumTile(
    name: String,
    artist: String,
    cover: Track?,
    viewModel: CollectionsViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val px = with(LocalDensity.current) { 200.dp.roundToPx() }
    val image by produceState<ImageBitmap?>(initialValue = null, cover?.uri) {
        value = cover?.let { viewModel.artwork(it.uri, px)?.asImageBitmap() }
    }
    Column(modifier.clip(MaterialTheme.shapes.large).clickable(onClick = onClick).padding(bottom = 8.dp)) {
        BoomArtwork(
            image = image,
            fallbackText = name,
            size = null,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        )
        Spacer(Modifier.height(8.dp))
        Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (artist.isNotEmpty()) {
            Text(
                artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EmptySection(message: Int) {
    Text(
        text = stringResource(message),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 32.dp),
    )
}
