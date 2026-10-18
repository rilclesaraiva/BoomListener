// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.boomlistener.core.common.ExternalLinks
import com.boomlistener.core.common.MediaPermissions
import com.boomlistener.core.common.openUrl
import com.boomlistener.core.common.shareLink
import com.boomlistener.core.common.openAppSettings
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
import com.boomlistener.core.designsystem.component.BoomSearchTopBar
import com.boomlistener.core.designsystem.component.BoomTopActions
import com.boomlistener.core.designsystem.component.ScrollToTopOnReselect
import androidx.compose.foundation.lazy.rememberLazyListState
import com.boomlistener.core.model.Track
import com.boomlistener.core.player.PlayerConnection
import com.boomlistener.data.deezer.DeezerData
import kotlinx.coroutines.launch
import com.boomlistener.data.library.LibraryData

@Composable
internal fun LibraryScreen(
    onOpenArtist: (artistName: String) -> Unit,
    onEditTags: (Track) -> Unit,
    onRecommend: (Track) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var addToPlaylist by remember { mutableStateOf<Track?>(null) }
    val viewModel: LibraryViewModel = viewModel {
        LibraryViewModel(
            LibraryData.repository(context.applicationContext),
            PlayerConnection.controller(context.applicationContext),
            songLink = DeezerData.repository(context.applicationContext)::youtubeMusicLinkForSong,
        )
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var hasPermission by remember { mutableStateOf(MediaPermissions.hasReadAudio(context)) }
    // Depois de duas recusas o Android não mostra mais o pedido; aí só dá
    // para liberar pelas configurações do sistema.
    var denials by rememberSaveable { mutableIntStateOf(0) }
    var searching by rememberSaveable { mutableStateOf(false) }

    // Confere de novo ao voltar das configurações do sistema e relê a
    // biblioteca (músicas novas podem ter chegado enquanto o app estava fechado).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasPermission = MediaPermissions.hasReadAudio(context)
    }
    LaunchedEffect(hasPermission) { if (hasPermission) viewModel.load() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
        if (!granted) denials++
    }

    val closeSearch = {
        searching = false
        viewModel.onQueryChange("")
    }
    BackHandler(enabled = searching) { closeSearch() }

    val searchHint = stringResource(R.string.feature_library_search_hint)
    val searchBar: @Composable () -> Unit = {
        BoomSearchTopBar(
            query = (uiState as? LibraryUiState.Success)?.query.orEmpty(),
            onQueryChange = viewModel::onQueryChange,
            onClose = closeSearch,
            placeholder = searchHint,
        )
    }

    BoomScreenScaffold(
        title = stringResource(R.string.feature_library_title),
        modifier = modifier,
        actions = {
            if (hasPermission) {
                IconButton(onClick = { searching = true }) {
                    Icon(BoomIcons.Search, contentDescription = stringResource(R.string.feature_library_search))
                }
            }
            BoomTopActions(onOpenHistory = onOpenHistory, onOpenSettings = onOpenSettings)
        },
        topBar = if (searching) searchBar else null,
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when {
            !hasPermission -> AudioPermissionRequest(
                denied = denials > 0,
                showSettings = denials >= 2,
                onRequest = { permissionLauncher.launch(MediaPermissions.readAudio) },
                onOpenSettings = { context.openAppSettings() },
                modifier = contentModifier,
            )

            else -> when (val state = uiState) {
                LibraryUiState.Loading -> BoomLoading(contentModifier)
                is LibraryUiState.Success -> TrackList(
                    state = state,
                    onShuffleAll = viewModel::shuffleAll,
                    loadArtwork = viewModel::artwork,
                    shareUrl = viewModel::shareUrl,
                    onOpenTrack = viewModel::play,
                    onOpenArtist = onOpenArtist,
                    onEditTags = onEditTags,
                    onAddToPlaylist = { addToPlaylist = it },
                    onRecommend = onRecommend,
                    contentPadding = padding,
                )
            }
        }
    }

    addToPlaylist?.let { track ->
        AddToPlaylistSheet(trackUris = listOf(track.uri), onDismiss = { addToPlaylist = null })
    }
}

@Composable
private fun TrackList(
    state: LibraryUiState.Success,
    onShuffleAll: () -> Unit,
    loadArtwork: suspend (Track, Int) -> android.graphics.Bitmap?,
    shareUrl: suspend (Track) -> String,
    onOpenTrack: (Track) -> Unit,
    onOpenArtist: (String) -> Unit,
    onEditTags: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onRecommend: (Track) -> Unit,
    contentPadding: PaddingValues,
) {
    val listState = rememberLazyListState()
    ScrollToTopOnReselect(LIBRARY_ROUTE, listState)
    when {
        state.totalCount == 0 -> BoomPlaceholder(
            title = stringResource(R.string.feature_library_empty_title),
            message = stringResource(R.string.feature_library_empty_message),
            modifier = Modifier.padding(contentPadding),
        )

        state.tracks.isEmpty() -> BoomPlaceholder(
            title = stringResource(R.string.feature_library_no_results_title),
            message = stringResource(R.string.feature_library_no_results_message, state.query),
            modifier = Modifier.padding(contentPadding),
        )

        else -> Box(Modifier.fillMaxSize()) {
          LazyColumn(state = listState, contentPadding = contentPadding) {
            item(key = "header") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                ) {
                    Text(
                        text = pluralStringResource(R.plurals.feature_library_count, state.tracks.size, state.tracks.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    FilledTonalButton(onClick = onShuffleAll) {
                        Icon(BoomIcons.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.feature_library_shuffle_all))
                    }
                }
            }
            items(state.tracks, key = { it.id }) { track ->
                TrackItem(
                    track = track,
                    version = state.version,
                    loadArtwork = loadArtwork,
                    shareUrl = shareUrl,
                    onClick = { onOpenTrack(track) },
                    onOpenArtist = track.artist?.let { name -> { onOpenArtist(name) } },
                    onEditTags = { onEditTags(track) },
                    onAddToPlaylist = { onAddToPlaylist(track) },
                    onRecommend = { onRecommend(track) },
                )
            }
          }
            // Arrastar pela direita pula pela lista, mostrando a letra.
            BoomFastScroller(
                state = listState,
                label = { index -> boomIndexLabel(state.tracks.getOrNull((index - 1).coerceAtLeast(0))?.title) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding()),
            )
        }
    }
}

@Composable
private fun TrackItem(
    track: Track,
    version: Int,
    loadArtwork: suspend (Track, Int) -> android.graphics.Bitmap?,
    shareUrl: suspend (Track) -> String,
    onClick: () -> Unit,
    onOpenArtist: (() -> Unit)?,
    onEditTags: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onRecommend: () -> Unit,
) {
    val sizePx = with(LocalDensity.current) { BoomListArtworkSize.roundToPx() }
    // [version] muda quando alguma tag foi editada: a capa é lida de novo.
    val artwork by produceState<ImageBitmap?>(initialValue = null, track.uri, version) {
        value = loadArtwork(track, sizePx)?.asImageBitmap()
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val unknownArtist = stringResource(R.string.feature_library_unknown_artist)

    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { BoomArtwork(image = artwork, fallbackText = track.title) },
        headlineContent = { Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                text = listOfNotNull(track.artist ?: unknownArtist, track.album).joinToString(" • "),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            BoomMoreMenu(
                actions = listOfNotNull(
                    BoomMenuAction(stringResource(R.string.feature_library_add_to_playlist), onClick = onAddToPlaylist),
                    BoomMenuAction(stringResource(R.string.feature_library_recommend), onClick = onRecommend),
                    onOpenArtist?.let { BoomMenuAction(stringResource(R.string.feature_library_go_to_artist), onClick = it) },
                    BoomMenuAction(stringResource(R.string.feature_library_edit_tags), onClick = onEditTags),
                    BoomMenuAction(
                        label = stringResource(R.string.feature_library_duration, formatDuration(track.durationMs)),
                        enabled = false,
                        onClick = {},
                    ),
                ),
                onShare = {
                    scope.launch {
                        context.shareLink(
                            url = shareUrl(track),
                            title = listOfNotNull(track.title, track.artist).joinToString(" — "),
                        )
                    }
                },
                onYoutubeMusic = { context.openUrl(ExternalLinks.youtubeMusicSearch(track.title, track.artist)) },
            )
        },
    )
}

@Composable
private fun AudioPermissionRequest(
    denied: Boolean,
    showSettings: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoomPlaceholder(
        title = stringResource(R.string.feature_library_permission_title),
        message = stringResource(
            if (denied) R.string.feature_library_permission_denied
            else R.string.feature_library_permission_message,
        ),
        modifier = modifier,
    ) {
        if (showSettings) {
            Button(onClick = onOpenSettings) {
                Text(stringResource(R.string.feature_library_permission_settings))
            }
        } else {
            Button(onClick = onRequest) {
                Text(stringResource(R.string.feature_library_permission_button))
            }
        }
    }
}
