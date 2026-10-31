// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.app

import android.widget.Toast
import com.boomlistener.feature.artist.ARTIST_NAME_ARG
import com.boomlistener.feature.artist.ARTIST_ROUTE
import com.boomlistener.core.player.PlayerConnection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.runtime.collectAsState
import androidx.activity.compose.BackHandler
import com.boomlistener.core.common.popBackStackOnce
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.boomlistener.feature.nowplaying.playerSheetAnchor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import com.boomlistener.core.designsystem.component.LocalBottomOverlay
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
import com.boomlistener.feature.nowplaying.rememberArtistOpener
import com.boomlistener.core.datastore.PreferencesData
import com.boomlistener.core.designsystem.component.BackgroundBlurState
import com.boomlistener.core.designsystem.component.LocalBackgroundBlur
import com.boomlistener.core.designsystem.component.fractionBlur
import com.boomlistener.feature.releases.navigateToReleases
import com.boomlistener.feature.releases.releasesScreen
import com.boomlistener.feature.settings.SETTINGS_ROUTE
import com.boomlistener.feature.settings.UpdatePromptHost
import com.boomlistener.feature.history.HistorySongMenu
import com.boomlistener.feature.settings.settingsScreen
import com.boomlistener.core.designsystem.component.LocalTabReselect
import com.boomlistener.core.designsystem.theme.boomSpatialSpec
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.flow.MutableSharedFlow
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.first
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.feature.profile.profileScreen
import com.boomlistener.feature.profile.ProfileSongMenu
import com.boomlistener.core.designsystem.component.FavoriteShare
import com.boomlistener.core.designsystem.component.LocalFavoriteShare
import com.boomlistener.core.common.ShareTarget
import com.boomlistener.feature.tageditor.navigateToTagEditor
import com.boomlistener.feature.tageditor.tageditorScreen
import com.boomlistener.feature.history.navigateToHistory
import com.boomlistener.feature.settings.navigateToSettings
import com.boomlistener.feature.library.AddToPlaylistSheet
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.boomlistener.feature.history.historyScreen

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
    // Tocar de novo na aba aberta: a lista dela sobe até o topo.
    val reselect = remember { MutableSharedFlow<String>(extraBufferCapacity = 1) }

    // A barra de abas só aparece nas próprias abas; telas de dentro (artista,
    // álbum, playlist, editor, ajustes…) ficam com a tela toda. Ajustes e o
    // editor de tags também escondem o mini player.
    val route = currentDestination?.route.orEmpty()
    val onTab = route.isEmpty() || route in topLevelRoutes
    val inSettings = route == SETTINGS_ROUTE || route.startsWith("$SETTINGS_ROUTE/")
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
    // Desfoque do fundo: a tela que fica por baixo desfoca (Ajustes → Aparência).
    val backgroundBlur = remember { BackgroundBlurState() }
    val userPrefs by remember { PreferencesData.repository(context).preferences }
        .collectAsStateWithLifecycle(initialValue = null)
    backgroundBlur.enabled = userPrefs?.backgroundBlur ?: true
    // Cada movimento conta ao desfoque qual tela fica por baixo (a direção é a mesma das animações).
    fun AnimatedContentTransitionScope<NavBackStackEntry>.tellBlur() {
        backgroundBlur.tabSwitch = isTabSwitch()
        backgroundBlur.goingBack = goingBack()
    }
    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        tellBlur()
        when {
            isTabSwitch() -> fadeIn(fadeSpec)
            goingBack() -> backEnter
            else -> forwardEnter
        }
    }
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        tellBlur()
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
    // Ir para uma aba (barra de baixo).
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
    // Abrir um artista que já está aberto não empilha outra página igual.
    val openArtist: (String) -> Unit = { name ->
        val current = navController.currentBackStackEntry
        val same = current?.destination?.route == ARTIST_ROUTE && current.arguments?.getString(ARTIST_NAME_ARG) == name
        if (!same) navController.navigateToArtist(name)
    }
    // Nos menus das telas (Início, Biblioteca, Perfil…), música com vários
    // artistas abre um painel com cada um; o player tem o dele.
    val openSongArtist = rememberArtistOpener(openArtist)
    // Música num formato que o aparelho não lê: avisa (o player já pulou para a próxima).
    LaunchedEffect(Unit) {
        PlayerConnection.controller(context).unsupported.collect { title ->
            Toast.makeText(context, context.getString(R.string.app_unsupported_format, title), Toast.LENGTH_LONG).show()
        }
    }
    // Tocou no aviso de lançamento: abre a tela de Lançamentos (player fechado).
    val openReleasesRequest by AppRequests.openReleases.collectAsState()
    LaunchedEffect(openReleasesRequest) {
        if (openReleasesRequest) {
            AppRequests.openReleases.value = false
            // Abrindo o app pelo aviso: espera a navegação estar pronta.
            navController.currentBackStackEntryFlow.first()
            playerSheet.collapse()
            navController.navigateToReleases()
        }
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
                onOpenArtist = openSongArtist,
                onEditTags = { track -> navController.navigateToTagEditor(track.uri) },
                onOpenReleases = openReleases,
                onOpenSettings = openSettings,
            )
            profileScreen(
                onOpenArtist = openSongArtist,
                onOpenHistory = openHistory,
                onOpenSettings = openSettings,
                songMenu = ProfileSongMenu(
                    onOpenArtist = openSongArtist,
                    onEditTags = { uri -> navController.navigateToTagEditor(uri) },
                    onAddToPlaylist = { uri -> addToPlaylistUri = uri },
                ),
            )
            artistScreen(
                navController = navController,
                onBack = back,
                onOpenArtist = openSongArtist,
            )
            tageditorScreen(onBack = back)
            releasesScreen(
                onBack = back,
                onOpenAlbum = navController::navigateToAlbum,
                onOpenArtist = openSongArtist,
            )
            settingsScreen(navController = navController)
            historyScreen(
                onBack = back,
                songMenu = HistorySongMenu(
                    onOpenArtist = openSongArtist,
                    onEditTags = { uri -> navController.navigateToTagEditor(uri) },
                    onAddToPlaylist = { uri -> addToPlaylistUri = uri },
                ),
            )
        }
    }

    CompositionLocalProvider(
        LocalTabReselect provides reselect,
        LocalFavoriteShare provides favoriteShare,
        LocalBackgroundBlur provides backgroundBlur,
    ) {
    Box(Modifier.fillMaxSize()) {
      // Altura do mini player (0 sem música), que as telas usam como folga no fim.
      // Medida no próprio mini player (não no que anima a entrada e a saída):
      // assim ela não muda a cada quadro ao entrar nos Ajustes, o que fazia as
      // duas telas se refazerem (e desfocarem de novo) quadro a quadro.
      var miniPlayerMeasured by remember { mutableStateOf(0.dp) }
      val miniPlayerVisible = !inSettings && !inTagEditor
      val miniPlayerHeight = if (miniPlayerVisible) miniPlayerMeasured else 0.dp
      // Altura da barra de abas, medida nela mesma pelo mesmo motivo.
      var navBarMeasured by remember { mutableStateOf<Dp?>(null) }
      val density = LocalDensity.current
        Scaffold(
            bottomBar = {
                    AnimatedVisibility(
                        visible = onTab,
                        enter = expandVertically(barSpec) + fadeIn(barFade),
                        exit = shrinkVertically(barSpec) + fadeOut(barFade),
                    ) {
                    // Abrindo o player, a barra de abas desce junto, empurrada
                    // pelo cartão que cresce a partir do mini player.
                    NavigationBar(
                        Modifier
                            .onSizeChanged { navBarMeasured = with(density) { it.height.toDp() } }
                            .graphicsLayer { translationY = playerSheet.fraction * size.height },
                    ) {
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
            },
        ) { innerPadding ->
            // Cada tela cuida do próprio topo; daqui só a folga da barra de abas.
            // O mini player flutua por cima das telas, sem faixa de fundo atrás
            // dele: a página continua por baixo e as listas ganham folga no fim
            // (LocalBottomOverlay).
            // A folga salta direto para o valor final (com ou sem a barra), em
            // vez de acompanhar a barra encolhendo quadro a quadro.
            val bottom = navBarMeasured?.let { if (onTab) it else 0.dp } ?: innerPadding.calculateBottomPadding()
            Box(Modifier.fillMaxSize()) {
                CompositionLocalProvider(LocalBottomOverlay provides miniPlayerHeight) {
                    // Abrindo o player, o app por trás vai desfocando.
                    navGraph(Modifier.padding(bottom = bottom).fractionBlur(backgroundBlur.enabled, { playerSheet.fraction }))
                }
                AnimatedVisibility(
                    visible = miniPlayerVisible,
                    enter = expandVertically(barSpec) + fadeIn(barFade),
                    exit = shrinkVertically(barSpec) + fadeOut(barFade),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = bottom)
                        // O player em tela cheia cresce a partir daqui.
                        .playerSheetAnchor(playerSheet),
                ) {
                    MiniPlayer(
                        sheetState = playerSheet,
                        modifier = Modifier.onSizeChanged { miniPlayerMeasured = with(density) { it.height.toDp() } },
                    )
                }
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

        // Versão nova no GitHub: aviso em tela cheia ao abrir o app (no máximo uma consulta por hora).
        UpdatePromptHost()
    }
}
}
