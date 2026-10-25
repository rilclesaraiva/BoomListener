// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.boomlistener.feature.library

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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.boomlistener.core.designsystem.component.clickableWithMenu
import androidx.compose.runtime.MutableState
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.boomlistener.core.designsystem.component.BoomMosaicArtwork
import androidx.compose.foundation.layout.heightIn
import com.boomlistener.core.designsystem.component.tuckUnderStickyHeader
import com.boomlistener.core.designsystem.component.rememberStickyHeaderStuck
import com.boomlistener.core.designsystem.component.BoomStickyHeader
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.core.designsystem.component.BoomArtwork
import com.boomlistener.core.designsystem.component.BoomFastScroller
import com.boomlistener.core.designsystem.component.boomIndexLabel
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.designsystem.component.BoomListArtworkSize
import com.boomlistener.core.designsystem.component.BoomLoading
import com.boomlistener.core.designsystem.component.BoomScreenScaffold
import com.boomlistener.core.designsystem.component.BoomSearchField
import com.boomlistener.core.designsystem.component.BoomTopActions
import com.boomlistener.core.designsystem.component.BoomPageTitle
import androidx.compose.material3.IconButton
import com.boomlistener.core.designsystem.component.ScrollToTopOnReselect
import androidx.compose.foundation.lazy.rememberLazyListState
import com.boomlistener.core.model.ExternalArtist
import com.boomlistener.core.model.Track

/**
 * Aba Biblioteca: botões em pílula no topo com Gêneros, Artistas, Playlists e
 * Álbuns; tocar num deles mostra aquela seção embaixo.
 */
@Composable
internal fun CollectionsScreen(
    viewModel: CollectionsViewModel,
    onOpenGenre: (key: String) -> Unit,
    onOpenArtist: (name: String) -> Unit,
    onOpenAlbum: (key: String) -> Unit,
    onOpenPlaylist: (id: Long) -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenReleases: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val artistSearch by viewModel.artistSearch.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    ScrollToTopOnReselect(COLLECTIONS_ROUTE, listState)
    // Veio do "Ver todos" do Início: abre a pílula pedida, do começo.
    LaunchedEffect(Unit) {
        CollectionsRequest.kind.collect { kind ->
            if (kind != null) {
                viewModel.select(kind)
                CollectionsRequest.kind.value = null
                listState.scrollToItem(0)
            }
        }
    }

    val searchScope = rememberCoroutineScope()
    // Álbum inteiro (menu do álbum) a caminho de uma playlist.
    var addManyToPlaylist by remember { mutableStateOf<List<String>?>(null) }
    val albumMenu = AlbumMenuActions(
        play = viewModel::playTracks,
        shuffle = viewModel::shuffleTracks,
        addToPlaylist = { tracks -> addManyToPlaylist = tracks.map { it.uri } },
        openArtist = onOpenArtist,
    )
    addManyToPlaylist?.let { uris -> AddToPlaylistSheet(trackUris = uris, onDismiss = { addManyToPlaylist = null }) }
    val gridKinds by viewModel.gridKinds.collectAsStateWithLifecycle()
    BoomScreenScaffold(
        title = stringResource(R.string.feature_library_collections_title),
        modifier = modifier,
        // Sem barra presa na tela: o título é o topo da página e sobe junto.
        topBar = {},
    ) { padding ->
        when (val current = state) {
            CollectionsUiState.Loading -> BoomLoading(Modifier.padding(padding))
            is CollectionsUiState.Ready -> Box(Modifier.fillMaxSize()) {
                val grid = current.selected.storageKey in gridKinds
                val searching = artistSearch.query.isNotBlank()
                val entries = collectionEntries(
                    state = current,
                    searchQuery = artistSearch.query,
                    albumMenu = albumMenu,
                    onOpenGenre = onOpenGenre,
                    onOpenArtist = onOpenArtist,
                    onOpenAlbum = onOpenAlbum,
                    onOpenPlaylist = onOpenPlaylist,
                    onOpenFavorites = onOpenFavorites,
                )
              val artistSearchStuck by rememberStickyHeaderStuck(listState, SEARCH_ITEM_INDEX)
              var artistSearchPx by remember { mutableIntStateOf(0) }
              val artistSearchRoom = rememberSearchRoom(listState, SEARCH_ITEM_INDEX, entries to artistSearch)
              LazyColumn(state = listState, contentPadding = padding) {
                item(key = "pageTitle") {
                    BoomPageTitle(stringResource(R.string.feature_library_collections_title)) {
                        BoomTopActions(onOpenReleases = onOpenReleases, onOpenSettings = onOpenSettings)
                    }
                }
                item(key = "chips") {
                  // Em Artistas, a busca presa vem logo depois e fica por cima do fim deste item.
                  Column(if (current.selected == CollectionKind.Artists) Modifier.tuckUnderStickyHeader() else Modifier) {
                    // Pílulas e, na ponta, grade/lista (lembrado por pílula).
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { CategoryChips(current, onSelect = viewModel::select) }
                        IconButton(onClick = { viewModel.setGrid(current.selected, !grid) }) {
                            if (grid) {
                                Icon(BoomIcons.ListView, contentDescription = stringResource(R.string.feature_library_show_list))
                            } else {
                                Icon(BoomIcons.GridView, contentDescription = stringResource(R.string.feature_library_show_grid))
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                  }
                }
                when (current.selected) {
                    CollectionKind.Genres -> {
                        if (current.genres.isEmpty()) item(key = "empty") { EmptySection(R.string.feature_library_genres_empty) }
                        groupItems(entries, grid, viewModel)
                    }
                    CollectionKind.Artists -> {
                        // Busca em pílula: os seus artistas e, do Deezer, os que você não tem.
                        // Presa no topo, como a do Início: começa com uma faixa do
                        // tamanho da das notificações, na cor da página, então
                        // digitando ela fica logo abaixo dessa faixa (sólida), nunca
                        // por baixo dela.
                        stickyHeader(key = "artistSearch") {
                            BoomStickyHeader(stuck = artistSearchStuck, modifier = Modifier.onSizeChanged { artistSearchPx = it.height }) {
                                    BoomSearchField(
                                        query = artistSearch.query,
                                        // Cada letra traz a tela de volta até a busca (logo abaixo das pílulas).
                                        onQueryChange = { text ->
                                            val firstLetter = artistSearch.query.isEmpty() && text.isNotEmpty()
                                            viewModel.onArtistQueryChange(text)
                                            listState.bringToSearch(SEARCH_ITEM_INDEX, firstLetter, searchScope)
                                        },
                                        placeholder = stringResource(R.string.feature_library_search_artists),
                                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 40.dp, top = 4.dp, bottom = 8.dp),
                                    )
                            }
                        }
                        if (current.artists.isEmpty() && !searching) {
                            item(key = "empty") { EmptySection(R.string.feature_library_artists_empty) }
                        }
                        groupItems(entries, grid, viewModel)
                        if (searching) {
                            onlineArtists(
                                search = artistSearch,
                                online = onlineArtistsNotOwned(artistSearch.online, current.artists),
                                viewModel = viewModel,
                                onOpenArtist = onOpenArtist,
                            )
                            // Sem este espaço, poucos resultados deixariam a lista mais curta
                            // que a tela e ela desceria a cada letra (a busca pularia de lugar).
                            searchRoomItem(artistSearchRoom)
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
                        // Favoritas vêm sempre primeiro (o coração no player e nos menus).
                        groupItems(entries, grid, viewModel)
                        if (current.playlists.isEmpty()) item(key = "empty") { EmptySection(R.string.feature_library_playlists_empty) }
                    }
                    CollectionKind.Albums -> {
                        if (current.albums.isEmpty()) item(key = "empty") { EmptySection(R.string.feature_library_albums_empty) }
                        groupItems(entries, grid, viewModel)
                    }
                }
              }
                // Letra de cada linha da lista, na mesma ordem dos itens acima.
                val labels = remember(current, artistSearch, grid) { collectionLabels(current, artistSearch, entries, grid) }
                // A barrinha é só dos grupos: o trilho começa logo abaixo do que vem
                // antes deles (pílulas, ou a busca em Artistas), onde quer que isso
                // esteja na tela, e termina no fim da lista. Nunca sobe por cima.
                val artists = current.selected == CollectionKind.Artists
                val lastHeaderIndex = collectionHeaderCount(current.selected) - 1
                val headerBottomPx by remember(listState, lastHeaderIndex) {
                    derivedStateOf {
                        val header = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == lastHeaderIndex }
                        header?.let { it.offset + it.size } ?: 0
                    }
                }
                val topPx = maxOf(headerBottomPx, if (artists) artistSearchPx else 0)
                // Balão de letra só em lista (em ordem alfabética) e nunca buscando;
                // em grade não faz sentido.
                val showLabel = !grid && !(artists && searching)
                BoomFastScroller(
                    state = listState,
                    label = { index -> if (showLabel) labels.getOrNull(index) ?: labels.firstOrNull { it != null } else null },
                    // Aqui as listas costumam ser curtas: basta rolar para ela aparecer.
                    minItems = 0,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(
                            top = with(LocalDensity.current) { topPx.toDp() }.coerceAtLeast(padding.calculateTopPadding()),
                            bottom = padding.calculateBottomPadding(),
                        ),
                )
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

/**
 * Botões em pílula no topo (Gêneros, Artistas, Playlists, Álbuns): o
 * escolhido fica preenchido e mostra a seção dele embaixo.
 */
@Composable
private fun CategoryChips(state: CollectionsUiState.Ready, onSelect: (CollectionKind) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
    ) {
        items(CollectionKind.entries, key = { it.name }) { kind ->
            val label = stringResource(
                when (kind) {
                    CollectionKind.Genres -> R.string.feature_library_genres
                    CollectionKind.Artists -> R.string.feature_library_artists
                    CollectionKind.Playlists -> R.string.feature_library_playlists
                    CollectionKind.Albums -> R.string.feature_library_albums
                },
            )
            val selected = state.selected == kind
            FilterChip(
                selected = selected,
                onClick = { onSelect(kind) },
                label = { Text(label, style = MaterialTheme.typography.labelLarge) },
                leadingIcon = if (selected) {
                    { Icon(BoomIcons.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                } else {
                    null
                },
                shape = CircleShape,
            )
        }
    }
}

/** "12 músicas", para o topo dos menus. */
@Composable
private fun countText(count: Int): String = pluralStringResource(R.plurals.feature_library_count, count, count)

@Composable
private fun GroupRow(
    name: String,
    subtitle: String,
    covers: List<Track>,
    viewModel: CollectionsViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    round: Boolean = false,
    fallbackIcon: ImageVector? = null,
    /** O menu do grupo, aberto ao segurar a linha. */
    menu: (@Composable (open: MutableState<Boolean>, image: ImageBitmap?) -> Unit)? = null,
) {
    val px = with(LocalDensity.current) { BoomListArtworkSize.roundToPx() }
    val images = rememberCovers(covers, px, viewModel)
    val image = images.firstOrNull()
    val menuOpen = remember { mutableStateOf(false) }
    menu?.invoke(menuOpen, image)
    ListItem(
        modifier = if (menu != null) modifier.clickableWithMenu(menuOpen, onClick = onClick) else modifier.clickable(onClick = onClick),
        leadingContent = {
            if (image == null && fallbackIcon != null && covers.size < MOSAIC_SIZE) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.size(BoomListArtworkSize),
                ) {
                    Box(contentAlignment = Alignment.Center) { Icon(fallbackIcon, contentDescription = null) }
                }
            } else if (covers.size >= MOSAIC_SIZE) {
                BoomMosaicArtwork(images)
            } else {
                BoomArtwork(
                    image = image,
                    fallbackText = name,
                    modifier = if (round) Modifier.clip(CircleShape) else Modifier,
                )
            }
        },
        headlineContent = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
    )
}

@Composable
private fun AlbumTile(
    name: String,
    artist: String,
    covers: List<Track>,
    viewModel: CollectionsViewModel,
    /** O menu do grupo, aberto ao segurar o bloco. */
    menu: @Composable (open: MutableState<Boolean>, image: ImageBitmap?) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Foto redonda (artista). */
    round: Boolean = false,
    /** Ícone no lugar da capa quando o grupo não tem nenhuma (playlist vazia, Favoritas). */
    fallbackIcon: ImageVector? = null,
) {
    val px = with(LocalDensity.current) { 200.dp.roundToPx() }
    val images = rememberCovers(covers, px, viewModel)
    val image = images.firstOrNull()
    val menuOpen = remember { mutableStateOf(false) }
    menu(menuOpen, image)
    // Canto menor e folga embaixo: o arredondado do toque não corta as letras.
    Column(modifier.clip(MaterialTheme.shapes.medium).clickableWithMenu(menuOpen, onClick = onClick).padding(bottom = 8.dp)) {
        if (image == null && fallbackIcon != null && covers.size < MOSAIC_SIZE) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            ) {
                Box(contentAlignment = Alignment.Center) { Icon(fallbackIcon, contentDescription = null, modifier = Modifier.size(48.dp)) }
            }
        } else if (covers.size >= MOSAIC_SIZE) {
            BoomMosaicArtwork(images, size = null, modifier = Modifier.fillMaxWidth().aspectRatio(1f))
        } else {
            BoomArtwork(
                image = image,
                fallbackText = name,
                size = null,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).then(if (round) Modifier.clip(CircleShape) else Modifier),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        if (artist.isNotEmpty()) {
            Text(
                artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp),
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

/** O rótulo do índice de cada item da lista (null nos que não são grupo). */
/** Quantas linhas vêm antes dos grupos (título, pílulas e, em Artistas, a busca ou o botão de nova playlist). */
private fun collectionHeaderCount(kind: CollectionKind): Int = when (kind) {
    CollectionKind.Artists, CollectionKind.Playlists -> 3
    CollectionKind.Genres, CollectionKind.Albums -> 2
}

private fun collectionLabels(
    state: CollectionsUiState.Ready,
    search: ArtistSearch,
    entries: List<GroupEntry>,
    grid: Boolean,
): List<String?> = buildList {
    add(null) // título da página
    add(null) // pílulas
    // Os grupos: uma letra por linha (na grade, cada linha tem dois).
    val groupLabels = (if (grid) entries.chunked(2).map { it.first() } else entries).map { boomIndexLabel(it.name) }
    when (state.selected) {
        CollectionKind.Genres -> {
            if (state.genres.isEmpty()) add(null)
            addAll(groupLabels)
        }
        CollectionKind.Artists -> {
            add(null) // busca
            val searching = search.query.isNotBlank()
            if (!searching && state.artists.isEmpty()) add(null)
            addAll(groupLabels)
            if (searching) {
                add(null) // título "No Deezer"
                val online = onlineArtistsNotOwned(search.online, state.artists)
                if (online.isEmpty()) add(null) // carregando / nada / falhou
                online.forEach { add(boomIndexLabel(it.name)) }
                add(null) // espaço do fim
            }
        }
        CollectionKind.Playlists -> {
            add(null) // botão de nova playlist
            addAll(groupLabels)
        }
        CollectionKind.Albums -> {
            if (state.albums.isEmpty()) add(null)
            addAll(groupLabels)
        }
    }
}

/**
 * Artistas do Deezer que você não tem no aparelho, abaixo dos seus. Tocar
 * abre a página do artista, como os do aparelho.
 */
private fun LazyListScope.onlineArtists(
    search: ArtistSearch,
    online: List<ExternalArtist>,
    viewModel: CollectionsViewModel,
    onOpenArtist: (String) -> Unit,
) {
    item(key = "onlineTitle") {
        Text(
            stringResource(R.string.feature_library_artists_online),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
        )
    }
    if (online.isEmpty()) {
        item(key = "onlineState") {
            // Altura fixa: girando ou com texto, o resto da lista não sobe e desce a cada letra.
            Box(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 20.dp), contentAlignment = Alignment.CenterStart) {
                when {
                    search.loading -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    search.failed -> Text(
                        stringResource(R.string.feature_library_artists_online_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    else -> Text(
                        stringResource(R.string.feature_library_artists_online_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
    items(online, key = { "dz:" + it.id }) { artist ->
        OnlineArtistRow(artist, viewModel, onClick = { onOpenArtist(artist.name) }, modifier = Modifier.animateItem())
    }
}

@Composable
private fun OnlineArtistRow(
    artist: ExternalArtist,
    viewModel: CollectionsViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val px = with(LocalDensity.current) { BoomListArtworkSize.roundToPx() }
    val image by produceState<ImageBitmap?>(initialValue = null, artist.pictureUrl) {
        value = artist.pictureUrl?.let { viewModel.artistPicture(it, px)?.asImageBitmap() }
    }
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        leadingContent = { BoomArtwork(image = image, fallbackText = artist.name, modifier = Modifier.clip(CircleShape)) },
        headlineContent = { Text(artist.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            if (artist.fans > 0) {
                Text(pluralStringResource(R.plurals.feature_library_fans, artist.fans.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), formatFans(artist.fans)))
            }
        },
    )
}

/** Número de fãs com a separação de milhar do idioma (1.234.567). */
private fun formatFans(fans: Long): String = java.text.NumberFormat.getIntegerInstance().format(fans)

/** A busca de artistas: logo depois do título da página e das pílulas. */
private const val SEARCH_ITEM_INDEX = 2

/** Um grupo da Biblioteca (gênero, artista, playlist, álbum), pronto para linha ou bloco. */
private class GroupEntry(
    val key: String,
    val name: String,
    val subtitle: String,
    val tracks: List<Track>,
    val round: Boolean = false,
    val fallbackIcon: ImageVector? = null,
    /** Playlists: capa montada com as de quatro álbuns ([mosaicCovers]). */
    val mosaic: Boolean = false,
    val onClick: () -> Unit,
    val menu: @Composable (open: MutableState<Boolean>, image: ImageBitmap?) -> Unit,
)

/** Os grupos da pílula escolhida (na busca de artistas, só os que batem). */
@Composable
private fun collectionEntries(
    state: CollectionsUiState.Ready,
    searchQuery: String,
    albumMenu: AlbumMenuActions,
    onOpenGenre: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onOpenFavorites: () -> Unit,
): List<GroupEntry> {
    val countOf: @Composable (Int) -> String = { pluralStringResource(R.plurals.feature_library_count, it, it) }
    return when (state.selected) {
        CollectionKind.Genres -> state.genres.map { group ->
            GroupEntry("g:" + group.key, group.name, countOf(group.tracks.size), group.tracks, onClick = { onOpenGenre(group.key) }) { open, image ->
                GroupMenuSheet(open, group.name, countText(group.tracks.size), group.tracks, image, albumMenu)
            }
        }
        CollectionKind.Artists -> {
            val local = if (searchQuery.isNotBlank()) matchingLocalArtists(state.artists, searchQuery) else state.artists
            local.map { group ->
                GroupEntry(
                    "a:" + group.key, group.name, countOf(group.tracks.size), group.tracks, round = true,
                    onClick = { onOpenArtist(group.name) },
                ) { open, image ->
                    GroupMenuSheet(
                        open, group.name, countText(group.tracks.size), group.tracks, image, albumMenu,
                        artistToOpen = group.name, youtubeQuery = group.name to null, round = true,
                    )
                }
            }
        }
        CollectionKind.Playlists -> {
            val favoritesName = stringResource(R.string.feature_library_favorites)
            listOf(
                GroupEntry(
                    "favorites", favoritesName, countOf(state.favorites.size), state.favorites,
                    fallbackIcon = BoomIcons.Favorite, mosaic = true, onClick = onOpenFavorites,
                ) { open, image ->
                    GroupMenuSheet(open, favoritesName, countText(state.favorites.size), state.favorites, image, albumMenu)
                },
            ) + state.playlists.map { summary ->
                GroupEntry(
                    "p:" + summary.playlist.id, summary.playlist.name, countOf(summary.tracks.size), summary.tracks,
                    fallbackIcon = BoomIcons.Playlist, mosaic = true, onClick = { onOpenPlaylist(summary.playlist.id) },
                ) { open, image ->
                    GroupMenuSheet(open, summary.playlist.name, countText(summary.tracks.size), summary.tracks, image, albumMenu)
                }
            }
        }
        CollectionKind.Albums -> state.albums.map { album ->
            GroupEntry(
                "al:" + album.key, album.name, listOfNotNull(album.subtitle, album.year?.toString()).joinToString(" · "), album.tracks,
                onClick = { onOpenAlbum(album.key) },
            ) { open, image ->
                AlbumMenuSheet(open, album.name, album.subtitle, album.tracks, image, albumMenu)
            }
        }
    }
}

/** Os grupos em lista (uma linha cada) ou em grade (dois blocos por linha). */
private fun LazyListScope.groupItems(entries: List<GroupEntry>, grid: Boolean, viewModel: CollectionsViewModel) {
    if (grid) {
        items(entries.chunked(2), key = { "grid:" + it.first().key }) { pair ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp).animateItem(),
            ) {
                pair.forEach { entry ->
                    AlbumTile(
                        name = entry.name,
                        artist = entry.subtitle,
                        covers = entry.covers(),
                        viewModel = viewModel,
                        round = entry.round,
                        fallbackIcon = entry.fallbackIcon,
                        menu = entry.menu,
                        onClick = entry.onClick,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    } else {
        items(entries, key = { it.key }) { entry ->
            GroupRow(
                name = entry.name,
                subtitle = entry.subtitle,
                covers = entry.covers(),
                viewModel = viewModel,
                round = entry.round,
                fallbackIcon = entry.fallbackIcon,
                menu = entry.menu,
                onClick = entry.onClick,
                modifier = Modifier.animateItem(),
            )
        }
    }
}

/** As capas do grupo: a do mosaico (playlists) ou só a primeira música. */
private fun GroupEntry.covers(): List<Track> =
    if (mosaic) mosaicCovers(tracks) else listOfNotNull(tracks.firstOrNull())

/** Carrega as capas de [covers] (na mesma ordem; null enquanto não chega). */
@Composable
private fun rememberCovers(covers: List<Track>, px: Int, viewModel: CollectionsViewModel): List<ImageBitmap?> {
    val uris = covers.map { it.uri }
    val images by produceState<List<ImageBitmap?>>(initialValue = List(uris.size) { null }, uris) {
        value = uris.map { uri -> viewModel.artwork(uri, px)?.asImageBitmap() }
    }
    return images
}
