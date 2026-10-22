// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.app

import com.boomlistener.feature.artist.ARTIST_NAME_ARG
import com.boomlistener.feature.artist.ARTIST_ROUTE
import com.boomlistener.feature.profile.ProfileAvatarButton
import com.boomlistener.feature.library.navigateToPlaylist
import com.boomlistener.feature.library.navigateToFavorites
import com.boomlistener.feature.library.NewPlaylistDialog
import com.boomlistener.feature.artist.ArtistSidePanel
import com.boomlistener.core.player.PlayerConnection
import com.boomlistener.core.designsystem.component.LocalWideLayout
import com.boomlistener.core.designsystem.component.LocalProfileButton
import com.boomlistener.core.designsystem.component.BoomIcons
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.runtime.collectAsState
import androidx.activity.compose.BackHandler
import com.boomlistener.core.common.popBackStackOnce
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.boomlistener.app.navigation.TopLevelDestination
import com.boomlistener.core.designsystem.theme.boomEffectsSpec
import com.boomlistener.core.designsystem.theme.boomScreenSpec
import com.boomlistener.feature.artist.artistScreen
import com.boomlistener.feature.artist.navigateToAlbum
import com.boomlistener.feature.artist.navigateToArtist
import com.boomlistener.feature.library.LIBRARY_ROUTE
import com.boomlistener.feature.library.libraryScreen
import com.boomlistener.feature.nowplaying.MiniPlayer
import com.boomlistener.feature.nowplaying.PlayerSheet
import com.boomlistener.feature.nowplaying.rememberPlayerSheetState
import com.boomlistener.feature.releases.navigateToReleases
import com.boomlistener.feature.releases.releasesScreen
import com.boomlistener.feature.settings.SETTINGS_ROUTE
import com.boomlistener.feature.settings.UpdatePromptHost
import com.boomlistener.feature.wellbeing.HistorySongMenu
import com.boomlistener.feature.settings.settingsScreen
import com.boomlistener.feature.wellbeing.WELLBEING_ROUTE
import com.boomlistener.core.designsystem.component.LocalTabReselect
import com.boomlistener.core.designsystem.theme.boomSpatialSpec
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.boomlistener.feature.profile.profileScreen
import com.boomlistener.feature.profile.ProfileSongMenu
import com.boomlistener.core.designsystem.component.FavoriteShare
import com.boomlistener.core.designsystem.component.LocalFavoriteShare
import com.boomlistener.core.common.ShareTarget
import com.boomlistener.feature.tageditor.navigateToTagEditor
import com.boomlistener.feature.tageditor.tageditorScreen
import com.boomlistener.feature.wellbeing.navigateToHistory
import com.boomlistener.feature.wellbeing.navigateToWellbeing
import com.boomlistener.feature.settings.navigateToSettings
import com.boomlistener.feature.library.AddToPlaylistSheet
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.boomlistener.feature.wellbeing.wellbeingScreen

private val topLevelRoutes = TopLevelDestination.entries.map { it.route }.toSet()

/** Troca entre abas da barra de baixo (e não abrir/fechar uma tela interna). */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch(): Boolean =
    initialState.destination.route in topLevelRoutes && targetState.destination.route in topLevelRoutes

/**
 * Voltar (e não abrir): a tela de destino está abaixo da atual na pilha, ou a
 * atual já saiu dela (voltar concluído).
 */
internal fun isBackNavigation(stack: List<NavBackStackEntry>, from: NavBackStackEntry, to: NavBackStackEntry): Boolean {
    val fromIndex = stack.indexOfFirst { it.id == from.id }
    val toIndex = stack.indexOfFirst { it.id == to.id }
    return fromIndex == -1 || (toIndex != -1 && toIndex < fromIndex)
}

/**
 * Esqueleto do app: mini player + barra de navegação + o grafo de telas, e o
 * player em tela cheia como painel por cima de tudo. É o ÚNICO lugar que
 * conhece todas as features e liga uma à outra (ver CLAUDE.md).
 */
@Composable
fun BoomListenerApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val playerSheet = rememberPlayerSheetState()
    // Música do player a adicionar numa playlist (a folha aparece por cima de tudo).
    var addToPlaylistUri by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current.applicationContext
    // O que está tocando (o painel do artista, na tela larga, depende disso).
    val playback by remember { PlayerConnection.controller(context) }.state.collectAsStateWithLifecycle()
    // Tocar de novo na aba aberta: a lista dela sobe até o topo.
    val reselect = remember { MutableSharedFlow<String>(extraBufferCapacity = 1) }

    // A barra de abas só aparece nas próprias abas; telas de dentro (artista,
    // álbum, playlist, editor, ajustes…) ficam com a tela toda. Ajustes e o
    // editor de tags também escondem o mini player.
    val route = currentDestination?.route.orEmpty()
    val onTab = route.isEmpty() || route in topLevelRoutes
    val inSettings = route == SETTINGS_ROUTE || route.startsWith("$SETTINGS_ROUTE/") || route == WELLBEING_ROUTE
    val inTagEditor = route.startsWith("tageditor")
    val barSpec = boomSpatialSpec<IntSize>()
    val barFade = boomEffectsSpec<Float>()

    // Telas internas entram deslizando por cima da anterior (que recua um
    // pouco e escurece) e saem deslizando de volta; trocar de aba só esmaece.
    // O "voltar" preditivo do sistema usa as mesmas animações de saída, então
    // a tela acompanha o dedo e termina o movimento ao soltar, sem salto.
    val slideSpec: FiniteAnimationSpec<IntOffset> = boomScreenSpec()
    val fadeSpec: FiniteAnimationSpec<Float> = boomEffectsSpec()
    // A direção (abrir ou voltar) é decidida aqui, pela pilha de telas, e não
    // pelo NavHost: no voltar preditivo ele às vezes usava as animações de
    // "abrir" (a tela de cima saía para a esquerda, transparente, por cima da
    // de baixo). Assim as quatro animações sempre concordam com o movimento.
    val forwardEnter = slideInHorizontally(slideSpec) { it }
    val forwardExit = slideOutHorizontally(slideSpec) { -it / 4 } + fadeOut(fadeSpec, targetAlpha = 0.4f)
    val backEnter = slideInHorizontally(slideSpec) { -it / 4 } + fadeIn(fadeSpec, initialAlpha = 0.4f)
    val backExit = slideOutHorizontally(slideSpec) { it }
    fun AnimatedContentTransitionScope<NavBackStackEntry>.goingBack(): Boolean =
        isBackNavigation(navController.currentBackStack.value, initialState, targetState)
    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        when {
            isTabSwitch() -> fadeIn(fadeSpec)
            goingBack() -> backEnter
            else -> forwardEnter
        }
    }
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        when {
            isTabSwitch() -> fadeOut(fadeSpec)
            goingBack() -> backExit
            else -> forwardExit
        }
    }
    val popEnter = enter
    val popExit = exit

    // App favorito para compartilhar: aparece como "Compartilhar com…" nos menus.
    val favoriteShareApp by ShareTarget.favorite(context).collectAsStateWithLifecycle()
    val favoriteShare = remember(favoriteShareApp) {
        favoriteShareApp?.let { app -> FavoriteShare(app.label) { ShareTarget.shareNextWith(app.packageName) } }
    }
    // Ir para uma aba (barra de baixo no celular, barra lateral na tela larga).
    fun openTab(destination: TopLevelDestination) {
        val selected = currentDestination?.hierarchy?.any { node ->
            node.route == destination.route || node.route?.startsWith(destination.route + "/") == true
        } == true
        when {
            // Já está nela: sobe a lista até o topo.
            route == destination.route -> reselect.tryEmit(destination.route)
            // Numa tela de dentro dela (ex.: um álbum): volta para o começo da aba.
            selected -> navController.popBackStack(destination.route, inclusive = false)
            else -> navController.navigate(destination.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
    // A aba marcada (também nas telas de dentro dela, ex.: um álbum da Biblioteca).
    val selectedTab = TopLevelDestination.entries.firstOrNull { destination ->
        currentDestination?.hierarchy?.any { node ->
            node.route == destination.route || node.route?.startsWith(destination.route + "/") == true
        } == true
    }
    var creatingPlaylist by remember { mutableStateOf(false) }
    // Abrir um artista que já está aberto não empilha outra página igual.
    val openArtist: (String) -> Unit = { name ->
        val current = navController.currentBackStackEntry
        val same = current?.destination?.route == ARTIST_ROUTE && current.arguments?.getString(ARTIST_NAME_ARG) == name
        if (!same) navController.navigateToArtist(name)
    }
    // Saiu do player aberto para outra tela (artista, editor): ao voltar até a
    // tela de onde saiu, o player abre de novo, como estava.
    var reopenPlayerOn by remember { mutableStateOf<String?>(null) }
    val openFromPlayer: (() -> Unit) -> Unit = { open ->
        val before = navController.currentBackStackEntry?.id
        open()
        val after = navController.currentBackStackEntry?.id
        if (after != before) reopenPlayerOn = before
    }
    LaunchedEffect(backStackEntry) {
        val target = reopenPlayerOn ?: return@LaunchedEffect
        val entry = backStackEntry ?: return@LaunchedEffect
        when {
            entry.id == target -> {
                reopenPlayerOn = null
                playerSheet.expand()
            }
            // A tela de origem saiu da pilha (trocou de aba, por exemplo): esquece.
            navController.currentBackStack.value.none { it.id == target } -> reopenPlayerOn = null
        }
    }
    // Na página de um artista (ou num álbum dele), a tela larga dá o espaço
    // todo para ela: a barra lateral recolhe e o painel do artista some.
    val onArtistPage = route == ARTIST_ROUTE || route.startsWith("album/")

    val navGraph: @Composable (Modifier) -> Unit = { navModifier ->
        NavHost(
            navController = navController,
            startDestination = LIBRARY_ROUTE,
            modifier = navModifier,
            enterTransition = enter,
            exitTransition = exit,
            popEnterTransition = popEnter,
            popExitTransition = popExit,
            // Sem isto o Android usa a animação padrão (a tela encolhe num
            // cartão e some de uma vez ao soltar o dedo).
            predictivePopEnterTransition = { popEnter(this) },
            predictivePopExitTransition = { popExit(this) },
        ) {
            val back: () -> Unit = { navController.popBackStackOnce() }
            val openHistory: () -> Unit = { navController.navigateToHistory() }
            val openReleases: () -> Unit = { navController.navigateToReleases() }
            val openSettings: () -> Unit = { navController.navigateToSettings() }
            libraryScreen(
                navController = navController,
                onOpenArtist = openArtist,
                onEditTags = { track -> navController.navigateToTagEditor(track.uri) },
                onOpenReleases = openReleases,
                onOpenSettings = openSettings,
            )
            profileScreen(
                onOpenArtist = openArtist,
                onOpenHistory = openHistory,
                onOpenSettings = openSettings,
                songMenu = ProfileSongMenu(
                    onOpenArtist = openArtist,
                    onEditTags = { uri -> navController.navigateToTagEditor(uri) },
                    onAddToPlaylist = { uri -> addToPlaylistUri = uri },
                ),
            )
            artistScreen(
                navController = navController,
                onBack = back,
                onOpenArtist = openArtist,
            )
            tageditorScreen(onBack = back)
            releasesScreen(
                onBack = back,
                onOpenAlbum = navController::navigateToAlbum,
                onOpenArtist = openArtist,
            )
            settingsScreen(
                navController = navController,
                onOpenWellbeing = navController::navigateToWellbeing,
            )
            wellbeingScreen(
                onBack = back,
                songMenu = HistorySongMenu(
                    onOpenArtist = openArtist,
                    onEditTags = { uri -> navController.navigateToTagEditor(uri) },
                    onAddToPlaylist = { uri -> addToPlaylistUri = uri },
                ),
            )
        }
    }

    CompositionLocalProvider(
        LocalTabReselect provides reselect,
        LocalFavoriteShare provides favoriteShare,
    ) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
      val wide = isWideWindow(maxWidth, maxHeight)
      val roomy = maxWidth >= 1000.dp
      // Na primeira vez numa tela larga: tablet abre tudo; celular deitado
      // (pouca largura) começa com a barra e o painel recolhidos.
      var sidebarExpanded by rememberSaveable { mutableStateOf(maxWidth >= 840.dp) }
      var artistPanelOpen by rememberSaveable { mutableStateOf(maxWidth >= 1000.dp) }
      CompositionLocalProvider(
          LocalWideLayout provides wide,
          LocalProfileButton provides { ProfileAvatarButton(onClick = { openTab(TopLevelDestination.Profile) }) },
      ) {
        if (wide) {
            // Tela larga: barra lateral | telas + mini player | artista tocando.
            Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxSize()) {
                    WideSidebar(
                        expanded = sidebarExpanded && !onArtistPage,
                        onToggle = { sidebarExpanded = !(sidebarExpanded && !onArtistPage) },
                        selected = selectedTab,
                        onOpenTab = ::openTab,
                        onOpenReleases = { navController.navigateToReleases() },
                        onNewPlaylist = { creatingPlaylist = true },
                        onOpenFavorites = { navController.navigateToFavorites() },
                        onOpenArtist = openArtist,
                    )
                    // Sem o painel à direita, a margem do sistema daquele lado fica com as telas.
                    val centerInsets = if (playback.item?.artist.isNullOrBlank() || onArtistPage) {
                        WindowInsets.safeDrawing.only(WindowInsetsSides.End)
                    } else {
                        WindowInsets(0, 0, 0, 0)
                    }
                    Column(Modifier.weight(1f).fillMaxHeight().windowInsetsPadding(centerInsets)) {
                        navGraph(Modifier.weight(1f).fillMaxWidth())
                        AnimatedVisibility(
                            visible = !inSettings && !inTagEditor,
                            enter = expandVertically(barSpec) + fadeIn(barFade),
                            exit = shrinkVertically(barSpec) + fadeOut(barFade),
                        ) {
                            MiniPlayer(
                                sheetState = playerSheet,
                                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
                            )
                        }
                    }
                    // Só com música tocando: o artista dela (recolhível).
                    val playingArtist = playback.item?.artist?.takeIf { it.isNotBlank() && !onArtistPage }
                    if (playingArtist != null) {
                        val panelInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.End + WindowInsetsSides.Vertical)
                        if (artistPanelOpen) {
                            ArtistSidePanel(
                                artistName = playingArtist,
                                onOpenArtist = openArtist,
                                onCollapse = { artistPanelOpen = false },
                                modifier = Modifier
                                    .width(if (roomy) 340.dp else 300.dp)
                                    .fillMaxHeight()
                                    .windowInsetsPadding(panelInsets)
                                    .padding(top = 8.dp, end = 8.dp, bottom = 8.dp),
                            )
                        } else {
                            Column(Modifier.fillMaxHeight().windowInsetsPadding(panelInsets).padding(top = 8.dp, end = 4.dp)) {
                                IconButton(onClick = { artistPanelOpen = true }) {
                                    Icon(BoomIcons.ChevronLeft, contentDescription = stringResource(R.string.wide_show_artist))
                                }
                            }
                        }
                    }
                }
            }
        } else {
        Scaffold(
            bottomBar = {
                Column {
                    AnimatedVisibility(
                        visible = !inSettings && !inTagEditor,
                        enter = expandVertically(barSpec) + fadeIn(barFade),
                        exit = shrinkVertically(barSpec) + fadeOut(barFade),
                    ) {
                        MiniPlayer(sheetState = playerSheet)
                    }
                    AnimatedVisibility(
                        visible = onTab,
                        enter = expandVertically(barSpec) + fadeIn(barFade),
                        exit = shrinkVertically(barSpec) + fadeOut(barFade),
                    ) {
                    NavigationBar {
                        TopLevelDestination.entries.forEach { destination ->
                            NavigationBarItem(
                                selected = destination == selectedTab,
                                onClick = { openTab(destination) },
                                icon = { Icon(destination.icon, contentDescription = null) },
                                label = { Text(stringResource(destination.label)) },
                            )
                        }
                    }
                    }
                }
            },
        ) { innerPadding ->
            // Cada tela cuida do próprio topo; daqui só a folga das barras de baixo.
            navGraph(Modifier.padding(bottom = innerPadding.calculateBottomPadding()))
        }
        }

        // Enquanto uma tela ainda está entrando ou saindo, o "voltar" espera:
        // um segundo voltar no meio da animação deixava as duas telas
        // sobrepostas e transparentes. Vem depois do NavHost, então tem
        // prioridade sobre ele (o gesto já começado continua com o NavHost).
        val entryLifecycle = remember(backStackEntry) {
            backStackEntry?.lifecycle?.currentStateFlow ?: MutableStateFlow(Lifecycle.State.RESUMED)
        }
        val entryState by entryLifecycle.collectAsState()
        BackHandler(enabled = !entryState.isAtLeast(Lifecycle.State.RESUMED)) { }

        // Por último: fica por cima de tudo e tem prioridade no "voltar".
        PlayerSheet(
            state = playerSheet,
            // Tocar no artista dentro do player: fecha o player e abre o perfil.
            onOpenArtist = { name ->
                playerSheet.collapse()
                openFromPlayer { openArtist(name) }
            },
            // Editar tags da música que está tocando: fecha o player e abre o editor.
            onEditTags = { uri ->
                playerSheet.collapse()
                openFromPlayer { navController.navigateToTagEditor(uri) }
            },
            onAddToPlaylist = { uri -> addToPlaylistUri = uri },
        )

        addToPlaylistUri?.let { uri ->
            AddToPlaylistSheet(trackUris = listOf(uri), onDismiss = { addToPlaylistUri = null })
        }

        if (creatingPlaylist) {
            NewPlaylistDialog(
                onCreated = { id -> navController.navigateToPlaylist(id) },
                onDismiss = { creatingPlaylist = false },
            )
        }

        // Versão nova no GitHub: aviso em tela cheia (no máximo uma consulta a cada 6 h).
        UpdatePromptHost()
      }
    }
}
}
