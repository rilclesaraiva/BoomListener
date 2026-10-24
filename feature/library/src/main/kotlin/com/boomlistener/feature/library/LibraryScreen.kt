// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.boomlistener.data.library.TrackGroup
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
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
import com.boomlistener.core.designsystem.component.tuckUnderStickyHeader
import com.boomlistener.core.designsystem.component.rememberStickyHeaderStuck
import com.boomlistener.core.designsystem.component.BoomStickyHeader
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.RowScope
import com.boomlistener.core.designsystem.component.BoomPageTitle
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
import com.boomlistener.core.designsystem.component.BoomBouncingText
import com.boomlistener.core.designsystem.component.BoomMenuSheet
import com.boomlistener.core.designsystem.component.BoomMenuHeader
import com.boomlistener.core.designsystem.component.BoomMenuKind
import com.boomlistener.core.designsystem.component.BoomFastScroller
import com.boomlistener.core.designsystem.component.boomIndexLabel
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.designsystem.component.BoomListArtworkSize
import com.boomlistener.core.designsystem.component.BoomLoading
import com.boomlistener.core.designsystem.component.BoomMenuAction
import com.boomlistener.core.designsystem.component.BoomMoreMenu
import com.boomlistener.core.designsystem.component.BoomPlaceholder
import com.boomlistener.core.designsystem.component.BoomScreenScaffold
import com.boomlistener.core.designsystem.component.BoomSearchField
import com.boomlistener.core.designsystem.component.clickableWithMenu
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
    onOpenAlbum: (albumKey: String) -> Unit,
    onSeeAll: (CollectionKind) -> Unit,
    onEditTags: (Track) -> Unit,
    onOpenReleases: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var addToPlaylist by remember { mutableStateOf<Track?>(null) }
    // Álbum inteiro (menu do álbum) a caminho de uma playlist.
    var addManyToPlaylist by remember { mutableStateOf<List<String>?>(null) }
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

    // Com busca digitada, o "voltar" limpa a busca antes de sair.
    val query = (uiState as? LibraryUiState.Success)?.query.orEmpty()
    BackHandler(enabled = query.isNotEmpty()) { viewModel.onQueryChange("") }
    val deleteSong = rememberSongDeleter()
    val showDetails = rememberSongDetails()

    val title = stringResource(R.string.feature_library_title)
    val topActions: @Composable RowScope.() -> Unit = {
        BoomTopActions(onOpenReleases = onOpenReleases, onOpenSettings = onOpenSettings)
    }
    // Sem barra presa na tela: o título é o topo da página e sobe junto ao rolar.
    BoomScreenScaffold(
        title = title,
        modifier = modifier,
        topBar = {},
    ) { padding ->
        val contentModifier = Modifier.padding(padding).windowInsetsPadding(WindowInsets.statusBars)
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
                    onPlayAll = viewModel::playAll,
                    onPlayRecent = viewModel::playRecent,
                    onQueryChange = viewModel::onQueryChange,
                    onDelete = deleteSong,
                    onDetails = { showDetails(it.uri, it.title) },
                    onOpenAlbum = onOpenAlbum,
                    onSeeAll = onSeeAll,
                    loadArtwork = viewModel::artwork,
                    shareUrl = viewModel::shareUrl,
                    onOpenTrack = viewModel::play,
                    onOpenArtist = onOpenArtist,
                    onEditTags = onEditTags,
                    onAddToPlaylist = { addToPlaylist = it },
                    albumMenu = AlbumMenuActions(
                        play = viewModel::playTracks,
                        shuffle = viewModel::shuffleTracks,
                        addToPlaylist = { tracks -> addManyToPlaylist = tracks.map { it.uri } },
                        openArtist = onOpenArtist,
                    ),
                    contentPadding = padding,
                    title = { BoomPageTitle(title, actions = topActions) },
                )
            }
        }
    }

    addToPlaylist?.let { track ->
        AddToPlaylistSheet(trackUris = listOf(track.uri), onDismiss = { addToPlaylist = null })
    }
    addManyToPlaylist?.let { uris ->
        AddToPlaylistSheet(trackUris = uris, onDismiss = { addManyToPlaylist = null })
    }
}

@Composable
private fun TrackList(
    state: LibraryUiState.Success,
    onShuffleAll: () -> Unit,
    onPlayAll: () -> Unit,
    onPlayRecent: (Int) -> Unit,
    onQueryChange: (String) -> Unit,
    onDelete: (Track) -> Unit,
    onDetails: (Track) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onSeeAll: (CollectionKind) -> Unit,
    loadArtwork: suspend (Track, Int) -> android.graphics.Bitmap?,
    shareUrl: suspend (Track) -> String,
    onOpenTrack: (Track) -> Unit,
    onOpenArtist: (String) -> Unit,
    onEditTags: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    albumMenu: AlbumMenuActions,
    contentPadding: PaddingValues,
    title: @Composable () -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    ScrollToTopOnReselect(LIBRARY_ROUTE, listState)
    val menu = remember(shareUrl, onOpenArtist, onEditTags, onAddToPlaylist, onDelete, onDetails, onOpenAlbum) {
        SongMenu(shareUrl, onOpenArtist, onEditTags, onAddToPlaylist, onDelete, onDetails, onOpenAlbum)
    }
    when {
        state.totalCount == 0 -> BoomPlaceholder(
            title = stringResource(R.string.feature_library_empty_title),
            message = stringResource(R.string.feature_library_empty_message),
            modifier = Modifier.padding(contentPadding).windowInsetsPadding(WindowInsets.statusBars),
        )

        else -> Box(Modifier.fillMaxSize()) {
            val home = state.home
            // Itens antes da primeira música (para a letra da barra de rolagem).
            val headerItems = 3 + (if (home != null) 3 else 0)
            // Altura do "Músicas" preso no topo, para a barrinha começar abaixo dele.
            var songsHeaderPx by remember { mutableIntStateOf(0) }
            // Preso no topo: a faixa das notificações acima dele fica sólida.
            val songsHeaderStuck by rememberStickyHeaderStuck(listState, headerItems - 1)
            // O item logo antes do "Músicas" fica por baixo da faixa dele (sem vão).
            val tuck = Modifier.tuckUnderStickyHeader()
          // A lista vai até o topo da tela: a faixa das notificações fica
          // transparente e mostra a página rolando por trás. O "Músicas", preso
          // no topo, traz a própria faixa sólida (SongsHeader) e cobre essa área.
          // O título é a primeira linha e sobe junto ao rolar.
          LazyColumn(
              state = listState,
              contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
          ) {
            item(key = "title") { title() }
            item(key = "header") {
                // Aleatório e Tocar, com a contagem do outro lado.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    // Folga à direita: a barra de rolagem rápida fica nessa borda.
                    modifier = Modifier
                        .then(if (home == null) tuck else Modifier)
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 48.dp, top = 4.dp, bottom = 8.dp),
                ) {
                    FilledTonalIconButton(
                        onClick = onShuffleAll,
                        modifier = Modifier.size(56.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        ),
                    ) {
                        Icon(BoomIcons.Shuffle, contentDescription = stringResource(R.string.feature_library_shuffle_all))
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(onClick = onPlayAll, modifier = Modifier.height(56.dp)) {
                        Icon(BoomIcons.Play, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.feature_library_play), style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = pluralStringResource(R.plurals.feature_library_count, state.tracks.size, state.tracks.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (home != null) {
                item(key = "recent") {
                    HomeSectionTitle(stringResource(R.string.feature_library_recently_added))
                    RecentGrid(home.recent, state.version, loadArtwork, menu, onPlayRecent)
                }
                item(key = "albums") {
                    if (home.albums.isNotEmpty()) {
                        HomeSectionTitle(
                            stringResource(R.string.feature_library_albums),
                            action = { SeeAllButton { onSeeAll(CollectionKind.Albums) } },
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                        ) {
                            items(home.albums, key = { it.key }) { album ->
                                HomeAlbum(album, state.version, loadArtwork, albumMenu, onClick = { onOpenAlbum(album.key) })
                            }
                        }
                    }
                }
                item(key = "artists") {
                  Column(tuck) {
                    if (home.artists.isNotEmpty()) {
                        HomeSectionTitle(
                            stringResource(R.string.feature_library_artists),
                            action = { SeeAllButton { onSeeAll(CollectionKind.Artists) } },
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                        ) {
                            items(home.artists, key = { it.key }) { artist ->
                                HomeArtist(artist, state.version, loadArtwork, albumMenu, onClick = { onOpenArtist(artist.name) })
                            }
                        }
                    }
                  }
                }
            }
            // "Músicas" com a busca ao lado: fica sempre, mesmo buscando.
            // Fica presa no topo ao rolar: a busca está sempre à mão.
            stickyHeader(key = "songsTitle") {
                SongsHeader(
                    stuck = songsHeaderStuck,
                    modifier = Modifier.onSizeChanged { songsHeaderPx = it.height },
                    query = state.query,
                    // Cada letra digitada traz a tela de volta até a busca (a pessoa
                    // pode ter rolado para cima no meio da busca).
                    onQueryChange = { text ->
                        onQueryChange(text)
                        scope.launch { listState.bringToSearch(headerItems - 1) }
                    },
                    // Ao tocar na busca, a lista sobe até ela (os resultados aparecem logo abaixo).
                    onFocused = { scope.launch { listState.animateScrollToItem(headerItems - 1) } },
                )
            }
            if (state.tracks.isEmpty()) {
                item(key = "noResults") {
                    Text(
                        stringResource(R.string.feature_library_no_results_message, state.query),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    )
                }
            }
            items(state.tracks, key = { it.id }) { track ->
                TrackItem(
                    track = track,
                    version = state.version,
                    loadArtwork = loadArtwork,
                    menu = menu,
                    onClick = { onOpenTrack(track) },
                )
            }
            // Buscando, os resultados podem ser poucos: sem este espaço no fim, a
            // lista ficaria mais curta que a tela e desceria sozinha (a busca
            // pularia de lugar a cada letra). Com ele, a busca fica onde está.
            if (state.query.isNotEmpty()) {
                item(key = "searchRoom") { Spacer(Modifier.fillParentMaxHeight()) }
            }
          }
            // Arrastar pela direita pula pela lista, mostrando a letra.
            BoomFastScroller(
                state = listState,
                label = { index -> boomIndexLabel(state.tracks.getOrNull((index - headerItems).coerceAtLeast(0))?.title) },
                // Só na seção "Músicas": o topo (recentes, álbuns, artistas) fica de fora.
                startIndex = headerItems - 1,
                // A barrinha começa abaixo do "Músicas" preso no topo (que inclui a
                // faixa das notificações): ela é só da lista, não do cabeçalho.
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = with(LocalDensity.current) { songsHeaderPx.toDp() })
                    .padding(bottom = contentPadding.calculateBottomPadding()),
            )
        }
    }
}

/**
 * Rola até a barra de busca (item [index]) ficar no topo. Perto, anima; longe,
 * pula direto (animar até um item fora da tela anda em etapas e trava).
 */
internal suspend fun LazyListState.bringToSearch(index: Int) {
    // Já passou da busca (ela está presa no topo e a lista rolou nos
    // resultados): os resultados voltam ao começo.
    if (firstVisibleItemIndex > index) {
        scrollToItem(index)
        return
    }
    // A busca já está no topo, ou quase: não mexe na página. Rolar a cada
    // letra fazia a página tremer e recriava o campo de texto (segurar o
    // apagar só apagava uma letra).
    if (firstVisibleItemIndex == index) return
    val visible = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    when {
        visible == null -> scrollToItem(index)
        visible.offset <= NEAR_TOP_PX -> Unit
        // Longe do topo: sobe até ela, com alvo absoluto.
        else -> animateScrollToItem(index)
    }
}

/** Distância do topo (px) em que a busca já conta como "no topo". */
private const val NEAR_TOP_PX = 160

/** Título "Músicas" com a barra de busca ao lado. */
@Composable
private fun SongsHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onFocused: () -> Unit,
    stuck: Boolean,
    modifier: Modifier = Modifier,
) {
    // Preso no topo, fica embaixo da faixa das notificações: a faixa de cima
    // (do tamanho dela) deixa essa área sólida, na mesma cor do cabeçalho.
    // Fora do topo, ela é só um respiro acima do título.
    BoomStickyHeader(stuck = stuck, modifier = modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 40.dp, top = 12.dp, bottom = 8.dp),
    ) {
        Text(stringResource(R.string.feature_library_songs), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.width(16.dp))
        BoomSearchField(
            query = query,
            onQueryChange = onQueryChange,
            placeholder = stringResource(R.string.feature_library_search_songs),
            modifier = Modifier.weight(1f),
            onFocused = onFocused,
        )
    }
    }
}

/** "Ver todos": abre a aba Biblioteca na pílula da seção. */
@Composable
private fun SeeAllButton(onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(stringResource(R.string.feature_library_see_all))
        Spacer(Modifier.width(4.dp))
        Icon(BoomIcons.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun HomeSectionTitle(title: String, action: (@Composable () -> Unit)? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 20.dp, end = 40.dp, top = 20.dp, bottom = 8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

/** Capa de um item do topo do Início (relê quando as tags mudam). */
@Composable
private fun rememberHomeCover(
    track: Track?,
    version: Int,
    size: Dp,
    loadArtwork: suspend (Track, Int) -> android.graphics.Bitmap?,
): ImageBitmap? {
    val px = with(LocalDensity.current) { size.roundToPx() }
    val image by produceState<ImageBitmap?>(initialValue = null, track?.uri, version) {
        value = track?.let { loadArtwork(it, px)?.asImageBitmap() }
    }
    return image
}

/** Adicionadas recentemente: três linhas que correm para o lado, numeradas. */
@Composable
private fun RecentGrid(
    recent: List<Track>,
    version: Int,
    loadArtwork: suspend (Track, Int) -> android.graphics.Bitmap?,
    menu: SongMenu,
    onPlay: (Int) -> Unit,
) {
    val rows = minOf(3, recent.size).coerceAtLeast(1)
    LazyHorizontalGrid(
        rows = GridCells.Fixed(rows),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = Modifier.fillMaxWidth().height(RecentRowHeight * rows + 4.dp * (rows - 1)),
    ) {
        itemsIndexed(recent, key = { _, track -> track.id }) { index, track ->
            val cover = rememberHomeCover(track, version, RecentCover, loadArtwork)
            val menuOpen = remember { mutableStateOf(false) }
            Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .width(RecentItemWidth)
                    .height(RecentRowHeight)
                    .clip(MaterialTheme.shapes.medium)
                    // Segurar abre o menu da música, como na lista.
                    .clickableWithMenu(menuOpen) { onPlay(index) },
            ) {
                BoomArtwork(image = cover, fallbackText = track.title, size = RecentCover)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    // Título comprido vai e volta devagar, em vez de ficar cortado.
                    BoomBouncingText(
                        "${index + 1}. ${track.title}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        listOfNotNull(formatDuration(track.durationMs), track.artist).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
                SongMenuView(track, menu, menuOpen, withButton = false, cover = cover)
            }
        }
    }
}

@Composable
private fun HomeAlbum(
    album: TrackGroup,
    version: Int,
    loadArtwork: suspend (Track, Int) -> android.graphics.Bitmap?,
    menu: AlbumMenuActions,
    onClick: () -> Unit,
) {
    val cover = rememberHomeCover(album.tracks.firstOrNull(), version, HomeTile, loadArtwork)
    // Segurar o álbum abre o menu dele.
    val menuOpen = remember { mutableStateOf(false) }
    AlbumMenuSheet(menuOpen, album.name, album.subtitle, album.tracks, cover, menu)
    // Canto menor e folga embaixo: o arredondado do toque não corta as letras.
    Column(Modifier.width(HomeTile).clip(MaterialTheme.shapes.medium).clickableWithMenu(menuOpen, onClick = onClick).padding(bottom = 8.dp)) {
        BoomArtwork(image = cover, fallbackText = album.name, size = HomeTile)
        Spacer(Modifier.height(8.dp))
        // Sempre duas linhas de nome e uma de artista (mesmo vazias): todos os
        // blocos têm a mesma altura e a página não muda de tamanho ao rolar.
        Text(
            album.name,
            style = MaterialTheme.typography.titleSmall,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Text(
            album.subtitle.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun HomeArtist(
    artist: TrackGroup,
    version: Int,
    loadArtwork: suspend (Track, Int) -> android.graphics.Bitmap?,
    menu: AlbumMenuActions,
    onClick: () -> Unit,
) {
    val cover = rememberHomeCover(artist.tracks.firstOrNull(), version, HomeTile, loadArtwork)
    // Segurar o artista abre o menu dele.
    val menuOpen = remember { mutableStateOf(false) }
    val count = artist.tracks.size
    GroupMenuSheet(
        menuOpen, artist.name, pluralStringResource(R.plurals.feature_library_count, count, count), artist.tracks, cover, menu,
        artistToOpen = artist.name, youtubeQuery = artist.name to null, round = true,
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(HomeTile).clip(MaterialTheme.shapes.medium).clickableWithMenu(menuOpen, onClick = onClick).padding(bottom = 8.dp),
    ) {
        BoomArtwork(image = cover, fallbackText = artist.name, size = HomeTile, modifier = Modifier.clip(CircleShape))
        Spacer(Modifier.height(8.dp))
        Text(
            artist.name,
            style = MaterialTheme.typography.titleSmall,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

private val RecentCover = 56.dp
private val RecentRowHeight = 64.dp
private val RecentItemWidth = 280.dp
private val HomeTile = 132.dp

/** O que o menu de uma música faz no Início (lista e adicionadas recentemente). */
internal class SongMenu(
    val shareUrl: suspend (Track) -> String,
    val onOpenArtist: (String) -> Unit,
    val onEditTags: (Track) -> Unit,
    val onAddToPlaylist: (Track) -> Unit,
    val onDelete: (Track) -> Unit,
    val onDetails: (Track) -> Unit,
    val onOpenAlbum: (albumKey: String) -> Unit,
)

/** O menu de uma música, igual em todo lugar: com o botão ⋮ ([withButton]) ou só ao segurar. */
@Composable
private fun SongMenuView(
    track: Track,
    menu: SongMenu,
    openState: MutableState<Boolean>,
    withButton: Boolean,
    cover: ImageBitmap?,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val actions = listOfNotNull(
        playNextAction(track),
        BoomMenuAction(stringResource(R.string.feature_library_add_to_playlist), kind = BoomMenuKind.AddToPlaylist) { menu.onAddToPlaylist(track) },
        favoriteAction(track.uri),
        track.album?.takeIf { it.isNotBlank() }?.let {
            BoomMenuAction(stringResource(R.string.feature_library_go_to_album), kind = BoomMenuKind.GoToAlbum) { menu.onOpenAlbum(track.albumId.toString()) }
        },
        track.artist?.let { name ->
            BoomMenuAction(stringResource(R.string.feature_library_go_to_artist), kind = BoomMenuKind.GoToArtist) { menu.onOpenArtist(name) }
        },
        BoomMenuAction(stringResource(R.string.feature_library_edit_tags), kind = BoomMenuKind.EditTags) { menu.onEditTags(track) },
        BoomMenuAction(stringResource(R.string.feature_library_details), kind = BoomMenuKind.Details) { menu.onDetails(track) },
        BoomMenuAction(stringResource(R.string.feature_library_delete_song), kind = BoomMenuKind.Delete) { menu.onDelete(track) },
    )
    val header = BoomMenuHeader(track.title, track.artist, cover)
    val onShare: () -> Unit = {
        scope.launch {
            context.shareLink(
                url = menu.shareUrl(track),
                title = listOfNotNull(track.title, track.artist).joinToString(" — "),
            )
        }
    }
    val onYoutubeMusic: () -> Unit = { context.openUrl(ExternalLinks.youtubeMusicSearch(track.title, track.artist)) }
    if (withButton) {
        BoomMoreMenu(actions = actions, onShare = onShare, onYoutubeMusic = onYoutubeMusic, header = header, openState = openState)
    } else {
        BoomMenuSheet(openState, actions, onShare = onShare, onYoutubeMusic = onYoutubeMusic, header = header)
    }
}

@Composable
private fun TrackItem(
    track: Track,
    version: Int,
    loadArtwork: suspend (Track, Int) -> android.graphics.Bitmap?,
    menu: SongMenu,
    onClick: () -> Unit,
) {
    val menuOpen = remember { mutableStateOf(false) }
    val sizePx = with(LocalDensity.current) { BoomListArtworkSize.roundToPx() }
    // [version] muda quando alguma tag foi editada: a capa é lida de novo.
    val artwork by produceState<ImageBitmap?>(initialValue = null, track.uri, version) {
        value = loadArtwork(track, sizePx)?.asImageBitmap()
    }
    val unknownArtist = stringResource(R.string.feature_library_unknown_artist)

    ListItem(
        // Segurar a música abre o mesmo menu dos três pontinhos.
        modifier = Modifier.clickableWithMenu(menuOpen, onClick = onClick),
        leadingContent = { BoomArtwork(image = artwork, fallbackText = track.title) },
        headlineContent = { Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                text = listOfNotNull(track.artist ?: unknownArtist, track.album).joinToString(" • "),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = { SongMenuView(track, menu, menuOpen, withButton = true, cover = artwork) },
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
