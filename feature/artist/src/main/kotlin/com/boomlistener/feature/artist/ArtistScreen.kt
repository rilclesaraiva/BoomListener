// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.artist

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.boomlistener.core.common.ExternalLinks
import com.boomlistener.core.common.openUrl
import com.boomlistener.core.common.shareText
import com.boomlistener.core.designsystem.component.BoomArtwork
import com.boomlistener.core.designsystem.component.BoomMoreMenu
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.designsystem.component.BoomListArtworkSize
import com.boomlistener.core.designsystem.component.BoomLoading
import com.boomlistener.core.designsystem.component.BoomTabs
import com.boomlistener.core.model.ArtistProfile
import com.boomlistener.core.model.ExternalArtist
import com.boomlistener.core.model.PopularTrack
import com.boomlistener.core.model.Release
import com.boomlistener.core.model.ReleaseType
import com.boomlistener.core.model.Track
import com.boomlistener.core.player.PlayerConnection
import com.boomlistener.data.deezer.DeezerData
import com.boomlistener.data.library.LibraryData
import com.boomlistener.data.releases.ReleasesData
import java.text.NumberFormat

private val HeaderHeight = 340.dp
private val AvatarSize = 96.dp

/**
 * Perfil do artista: cabeçalho com a foto (Deezer) esmaecendo no fundo, foto
 * redonda sobreposta, números, Seguir/aleatório/tocar e abas.
 */
@Composable
internal fun ArtistScreen(
    artistName: String,
    onBack: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (albumId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    val viewModel: ArtistViewModel = viewModel(key = "artist:$artistName") {
        ArtistViewModel(
            artistName = artistName,
            library = LibraryData.repository(context),
            deezer = DeezerData.repository(context),
            follows = ReleasesData.follows(context),
            player = PlayerConnection.controller(context),
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val ready = (state.profile as? ProfileState.Ready)?.profile
    val showTopBar by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 24.dp)) {
            item(key = "header") { Header(state, ready, viewModel) }
            item(key = "stats") { Stats(state, ready) }
            item(key = "actions") {
                Actions(
                    canFollow = ready != null,
                    following = state.following,
                    hasLocal = state.localTracks.isNotEmpty(),
                    onFollow = viewModel::toggleFollow,
                    onShuffle = viewModel::shuffle,
                    onPlay = { viewModel.play() },
                )
            }
            item(key = "tabs") {
                BoomTabs(
                    titles = listOf(
                        stringResource(R.string.feature_artist_tab_songs),
                        stringResource(R.string.feature_artist_tab_discography),
                        stringResource(R.string.feature_artist_tab_about),
                    ),
                    selected = tab,
                    onSelect = { tab = it },
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            when (tab) {
                0 -> songsTab(state, ready, viewModel, context)
                1 -> discographyTab(state, ready, viewModel, context, onOpenAlbum)
                else -> aboutTab(state, ready, viewModel, onOpenArtist)
            }
        }

        // Barra de cima: a seta sempre; o nome aparece quando o cabeçalho sai da tela.
        AnimatedVisibility(visible = showTopBar, enter = fadeIn(), exit = fadeOut()) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                    Row(Modifier.height(64.dp), verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.width(64.dp))
                        Text(
                            text = state.name,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
            modifier = Modifier.statusBarsPadding().padding(8.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(BoomIcons.Back, stringResource(R.string.feature_artist_back))
            }
        }
    }
}

@Composable
private fun Header(state: ArtistUiState, profile: ArtistProfile?, viewModel: ArtistViewModel) {
    val density = LocalDensity.current
    val widthPx = with(density) { 600.dp.roundToPx() }
    val photo = rememberRemoteImage(viewModel, profile?.artist?.pictureUrl, widthPx)
    val firstLocal = state.localTracks.firstOrNull()
    val avatarPx = with(density) { AvatarSize.roundToPx() }
    // A foto redonda usa a capa do lançamento mais recente (ou a de uma música sua).
    val avatarRemote = rememberRemoteImage(viewModel, profile?.releases?.firstOrNull()?.coverUrl, avatarPx)
    val avatarLocal = rememberLocalArtwork(viewModel, firstLocal, avatarPx)
    val surface = MaterialTheme.colorScheme.surface

    Box(Modifier.fillMaxWidth().height(HeaderHeight + AvatarSize / 2)) {
        Box(Modifier.fillMaxWidth().height(HeaderHeight)) {
            val background = photo ?: avatarLocal
            if (background != null) {
                Image(
                    bitmap = background,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.secondaryContainer))
            }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0.35f to Color.Transparent, 1f to surface),
                ),
            )
            Text(
                text = state.name,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, end = AvatarSize + 32.dp, bottom = 12.dp),
            )
        }
        BoomArtwork(
            image = avatarRemote ?: avatarLocal,
            fallbackText = state.name,
            size = AvatarSize,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp)
                .clip(CircleShape)
                .border(BorderStroke(3.dp, MaterialTheme.colorScheme.surface), CircleShape),
        )
    }
}

@Composable
private fun Stats(state: ArtistUiState, profile: ArtistProfile?) {
    val parts = buildList {
        add(pluralStringResource(R.plurals.feature_artist_local_songs, state.localTracks.size, state.localTracks.size))
        add(pluralStringResource(R.plurals.feature_artist_local_albums, state.localAlbums, state.localAlbums))
        if (profile != null) {
            val fans = NumberFormat.getIntegerInstance().format(profile.artist.fans)
            add(stringResource(R.string.feature_artist_fans, fans))
        }
    }
    Text(
        text = parts.joinToString(" • "),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp),
    )
}

@Composable
private fun Actions(
    canFollow: Boolean,
    following: Boolean,
    hasLocal: Boolean,
    onFollow: () -> Unit,
    onShuffle: () -> Unit,
    onPlay: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        if (following) {
            FilledTonalButton(onClick = onFollow, enabled = canFollow) {
                Icon(BoomIcons.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.feature_artist_following))
            }
        } else {
            OutlinedButton(onClick = onFollow, enabled = canFollow) {
                Text(stringResource(R.string.feature_artist_follow))
            }
        }
        Spacer(Modifier.weight(1f))
        FilledTonalIconButton(onClick = onShuffle, enabled = hasLocal, modifier = Modifier.size(48.dp)) {
            Icon(BoomIcons.Shuffle, stringResource(R.string.feature_artist_shuffle))
        }
        Spacer(Modifier.width(12.dp))
        FilledIconButton(
            onClick = onPlay,
            enabled = hasLocal,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.size(60.dp),
        ) {
            Icon(BoomIcons.Play, stringResource(R.string.feature_artist_play), Modifier.size(32.dp))
        }
    }
}

private fun LazyListScope.profileStatus(state: ArtistUiState, viewModel: ArtistViewModel) {
    when (state.profile) {
        ProfileState.Loading -> item { BoomLoading(Modifier.height(160.dp)) }
        ProfileState.NotFound -> item { Message(stringResource(R.string.feature_artist_not_found)) }
        ProfileState.Failed -> item {
            Message(stringResource(R.string.feature_artist_failed)) {
                TextButton(onClick = viewModel::loadProfile) { Text(stringResource(R.string.feature_artist_retry)) }
            }
        }
        is ProfileState.Ready -> Unit
    }
}

private fun LazyListScope.songsTab(
    state: ArtistUiState,
    profile: ArtistProfile?,
    viewModel: ArtistViewModel,
    context: Context,
) {
    if (state.localTracks.isNotEmpty()) {
        item { SectionTitle(stringResource(R.string.feature_artist_on_device)) }
        itemsIndexed(state.localTracks, key = { _, track -> "local-${track.id}" }) { index, track ->
            LocalTrackRow(track, viewModel, onClick = { viewModel.play(index) })
        }
    }
    if (profile != null) {
        if (profile.topTracks.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.feature_artist_popular)) }
            items(profile.topTracks, key = { "top-${it.id}" }) { popular ->
                PopularRow(popular, state.name, owned = popular.id in state.ownedPopular, viewModel, context)
            }
        }
    } else {
        profileStatus(state, viewModel)
    }
}

private fun LazyListScope.discographyTab(
    state: ArtistUiState,
    profile: ArtistProfile?,
    viewModel: ArtistViewModel,
    context: Context,
    onOpenAlbum: (Long) -> Unit,
) {
    if (profile == null) {
        profileStatus(state, viewModel)
        return
    }
    val groups = listOf(
        ReleaseType.Album to R.string.feature_artist_albums,
        ReleaseType.EP to R.string.feature_artist_eps,
        ReleaseType.Single to R.string.feature_artist_singles,
        ReleaseType.Compilation to R.string.feature_artist_compilations,
    )
    groups.forEach { (type, titleRes) ->
        val releases = profile.releases.filter { it.type == type }
        if (releases.isNotEmpty()) {
            item { SectionTitle(stringResource(titleRes)) }
            items(releases, key = { "release-${it.id}" }) { release ->
                ReleaseRow(release, state.name, owned = release.id in state.ownedReleases, viewModel, context, onOpen = { onOpenAlbum(release.id) })
            }
        }
    }
}

private fun LazyListScope.aboutTab(
    state: ArtistUiState,
    profile: ArtistProfile?,
    viewModel: ArtistViewModel,
    onOpenArtist: (String) -> Unit,
) {
    item {
        SectionTitle(stringResource(R.string.feature_artist_biography))
        Text(
            text = stringResource(R.string.feature_artist_biography_soon),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
    if (profile == null) {
        profileStatus(state, viewModel)
        return
    }
    if (profile.related.isNotEmpty()) {
        item {
            SectionTitle(stringResource(R.string.feature_artist_related))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(profile.related, key = { it.id }) { related ->
                    RelatedArtist(related, viewModel, onClick = { onOpenArtist(related.name) })
                }
            }
        }
    }
    item {
        Text(
            text = stringResource(R.string.feature_artist_source),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun Message(text: String, action: @Composable () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(24.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        action()
    }
}

@Composable
private fun LocalTrackRow(track: Track, viewModel: ArtistViewModel, onClick: () -> Unit) {
    val px = with(LocalDensity.current) { BoomListArtworkSize.roundToPx() }
    val artwork = rememberLocalArtwork(viewModel, track, px)
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { BoomArtwork(image = artwork, fallbackText = track.title) },
        headlineContent = { Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { track.album?.let { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
    )
}

@Composable
private fun PopularRow(
    track: PopularTrack,
    artistName: String,
    owned: Boolean,
    viewModel: ArtistViewModel,
    context: Context,
) {
    val px = with(LocalDensity.current) { BoomListArtworkSize.roundToPx() }
    val cover = rememberRemoteImage(viewModel, track.coverUrl, px)
    ListItem(
        modifier = Modifier.clickable { track.link?.let(context::openUrl) },
        leadingContent = { BoomArtwork(image = cover, fallbackText = track.title) },
        headlineContent = { Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                text = if (owned) stringResource(R.string.feature_artist_owned) else stringResource(R.string.feature_artist_missing),
                maxLines = 1,
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OwnedMark(owned)
                BoomMoreMenu(
                    onShare = track.link?.let { link -> { context.shareText(link) } },
                    onYoutubeMusic = { context.openUrl(ExternalLinks.youtubeMusicSearch(track.title, artistName)) },
                    onDeezer = track.link?.let { link -> { context.openUrl(link) } },
                )
            }
        },
    )
}

@Composable
private fun ReleaseRow(
    release: Release,
    artistName: String,
    owned: Boolean,
    viewModel: ArtistViewModel,
    context: Context,
    onOpen: () -> Unit,
) {
    val px = with(LocalDensity.current) { 64.dp.roundToPx() }
    val cover = rememberRemoteImage(viewModel, release.coverUrl, px)
    val typeName = stringResource(
        when (release.type) {
            ReleaseType.Album -> R.string.feature_artist_type_album
            ReleaseType.EP -> R.string.feature_artist_type_ep
            ReleaseType.Single -> R.string.feature_artist_type_single
            ReleaseType.Compilation -> R.string.feature_artist_type_compilation
        },
    )
    // Tocar abre o álbum aqui no app (com o que você tem e o que falta).
    ListItem(
        modifier = Modifier.clickable(onClick = onOpen),
        leadingContent = { BoomArtwork(image = cover, fallbackText = release.title, size = 64.dp) },
        headlineContent = { Text(release.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(listOfNotNull(release.year, typeName).joinToString(" • ")) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (owned) {
                    Icon(BoomIcons.Owned, stringResource(R.string.feature_artist_owned), tint = MaterialTheme.colorScheme.primary)
                }
                BoomMoreMenu(
                    onShare = release.link?.let { link -> { context.shareText(link) } },
                    onYoutubeMusic = { context.openUrl(ExternalLinks.youtubeMusicSearch(release.title, artistName)) },
                    onDeezer = release.link?.let { link -> { context.openUrl(link) } },
                )
            }
        },
    )
}

@Composable
private fun OwnedMark(owned: Boolean) {
    if (owned) {
        Icon(
            BoomIcons.Owned,
            contentDescription = stringResource(R.string.feature_artist_owned),
            tint = MaterialTheme.colorScheme.primary,
        )
    } else {
        Icon(
            BoomIcons.OpenExternal,
            contentDescription = stringResource(R.string.feature_artist_open_external),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RelatedArtist(artist: ExternalArtist, viewModel: ArtistViewModel, onClick: () -> Unit) {
    val size: Dp = 88.dp
    val px = with(LocalDensity.current) { size.roundToPx() }
    val photo = rememberRemoteImage(viewModel, artist.pictureUrl, px)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(size).clickable(onClick = onClick),
    ) {
        BoomArtwork(image = photo, fallbackText = artist.name, size = size, modifier = Modifier.clip(CircleShape))
        Spacer(Modifier.height(6.dp))
        Text(
            text = artist.name,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun rememberRemoteImage(viewModel: ArtistViewModel, url: String?, sizePx: Int): ImageBitmap? {
    val image by produceState<ImageBitmap?>(initialValue = null, url, sizePx) {
        value = url?.let { viewModel.remoteImage(it, sizePx)?.asImageBitmap() }
    }
    return image
}

@Composable
private fun rememberLocalArtwork(viewModel: ArtistViewModel, track: Track?, sizePx: Int): ImageBitmap? {
    val image by produceState<ImageBitmap?>(initialValue = null, track?.uri, sizePx) {
        value = track?.let { viewModel.localArtwork(it, sizePx)?.asImageBitmap() }
    }
    return image
}
