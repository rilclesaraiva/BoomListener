// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.artist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.boomlistener.core.common.ExternalLinks
import com.boomlistener.core.common.openUrl
import com.boomlistener.core.common.shareLink
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.boomlistener.core.designsystem.component.BoomArtwork
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.designsystem.component.BoomFloatingBackButton
import com.boomlistener.core.designsystem.component.BoomLoading
import com.boomlistener.core.designsystem.component.BoomMenuHeader
import com.boomlistener.core.designsystem.component.BoomMoreMenu
import com.boomlistener.core.designsystem.component.clickableWithMenu
import com.boomlistener.core.model.ReleaseType
import com.boomlistener.core.player.PlayerConnection
import com.boomlistener.data.deezer.DeezerData
import com.boomlistener.data.library.LibraryData

/**
 * Um álbum aberto: capa, dados, quantas músicas você tem e a lista. As que
 * você tem tocam aqui mesmo; as que faltam abrem fora do app (botão ou menu).
 */
@Composable
internal fun AlbumScreen(
    albumId: Long,
    artistName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext
    val viewModel: AlbumViewModel = viewModel(key = "album:$albumId") {
        AlbumViewModel(
            albumId = albumId,
            artistName = artistName,
            deezer = DeezerData.repository(app),
            library = LibraryData.repository(app),
            player = PlayerConnection.controller(app),
        )
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        when (val current = state) {
            AlbumUiState.Loading -> BoomLoading()
            AlbumUiState.Failed -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Text(stringResource(R.string.feature_artist_album_failed), color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = viewModel::load) { Text(stringResource(R.string.feature_artist_retry)) }
            }
            is AlbumUiState.Ready -> AlbumContent(current, viewModel)
        }
        BoomFloatingBackButton(onClick = onBack)
    }
}

@Composable
private fun AlbumContent(state: AlbumUiState.Ready, viewModel: AlbumViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val album = state.album
    val release = album.release
    val coverPx = with(LocalDensity.current) { 280.dp.roundToPx() }
    val cover = rememberAlbumImage(viewModel, release.coverUrl, coverPx)
    val typeName = stringResource(
        when (release.type) {
            ReleaseType.Album -> R.string.feature_artist_type_album
            ReleaseType.EP -> R.string.feature_artist_type_ep
            ReleaseType.Single -> R.string.feature_artist_type_single
            ReleaseType.Compilation -> R.string.feature_artist_type_compilation
        },
    )

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), modifier = Modifier.fillMaxSize()) {
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 56.dp, start = 24.dp, end = 24.dp),
            ) {
                BoomArtwork(image = cover, fallbackText = release.title, size = 260.dp)
                Spacer(Modifier.height(20.dp))
                Text(
                    text = release.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = listOfNotNull(album.artistName.ifBlank { null }, release.year, typeName).joinToString(" • "),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.feature_artist_album_owned, state.ownedCount, album.tracks.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                FilledTonalButton(onClick = { viewModel.playOwned() }, enabled = state.ownedCount > 0) {
                    Icon(BoomIcons.Play, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.feature_artist_album_play_owned))
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { release.link?.let(context::openUrl) }, enabled = release.link != null) {
                    Icon(BoomIcons.OpenExternal, stringResource(R.string.feature_artist_open_external))
                }
                BoomMoreMenu(
                    onShare = { scope.launch { context.shareLink(viewModel.shareUrl(release.link, release.title, album.artistName), release.title) } },
                    onYoutubeMusic = { context.openUrl(ExternalLinks.youtubeMusicSearch(release.title, album.artistName)) },
                    onDeezer = release.link?.let { link -> { context.openUrl(link) } },
                    header = BoomMenuHeader(release.title, album.artistName, cover),
                )
            }
        }
        itemsIndexed(album.tracks, key = { _, track -> track.id }) { index, track ->
            val local = state.local.getOrNull(index)
            val menuOpen = remember { mutableStateOf(false) }
            ListItem(
                // Segurar abre o mesmo menu dos três pontinhos.
                modifier = Modifier.clickableWithMenu(menuOpen) {
                    if (local != null) viewModel.playOwned(index) else track.link?.let(context::openUrl)
                },
                leadingContent = {
                    Text(
                        text = (index + 1).toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(28.dp),
                        textAlign = TextAlign.Center,
                    )
                },
                headlineContent = {
                    Text(
                        text = track.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (local != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                supportingContent = {
                    Text(
                        stringResource(
                            if (local != null) R.string.feature_artist_owned else R.string.feature_artist_missing,
                        ),
                    )
                },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (local != null) {
                            Icon(BoomIcons.Owned, stringResource(R.string.feature_artist_owned), tint = MaterialTheme.colorScheme.primary)
                        } else {
                            IconButton(onClick = { track.link?.let(context::openUrl) }, enabled = track.link != null) {
                                Icon(BoomIcons.OpenExternal, stringResource(R.string.feature_artist_open_external))
                            }
                        }
                        BoomMoreMenu(
                            onShare = { scope.launch { context.shareLink(viewModel.shareUrl(track.link, track.title, album.artistName), track.title) } },
                            onYoutubeMusic = { context.openUrl(ExternalLinks.youtubeMusicSearch(track.title, album.artistName)) },
                            onDeezer = track.link?.let { link -> { context.openUrl(link) } },
                            header = BoomMenuHeader(track.title, album.artistName, cover),
                            openState = menuOpen,
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun rememberAlbumImage(viewModel: AlbumViewModel, url: String?, sizePx: Int): ImageBitmap? {
    val image by produceState<ImageBitmap?>(initialValue = null, url, sizePx) {
        value = url?.let { viewModel.image(it, sizePx)?.asImageBitmap() }
    }
    return image
}
