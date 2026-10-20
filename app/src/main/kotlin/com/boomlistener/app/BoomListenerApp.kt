// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.app

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
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.boomlistener.data.social.LinkStatus
import com.boomlistener.data.social.SocialData
import com.boomlistener.feature.social.RecommendSheet
import com.boomlistener.feature.social.socialScreen
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
    // Música a recomendar para alguém (folha de Amigos por cima de tudo).
    var recommendUri by remember { mutableStateOf<String?>(null) }

    // Amigos: com o app na tela, fica ligado ao servidor por WebSocket (o
    // "ouvindo agora" e as recomendações chegam sozinhos). Sem conexão, consulta a cada minuto; em
    // segundo plano não consulta (só fica ligado se uma música estiver tocando
    // com o "ouvindo agora" ligado).
    val context = LocalContext.current.applicationContext
    val social = remember { SocialData.repository(context) }
    val socialAccount by social.account.collectAsStateWithLifecycle()
    // Número na aba Perfil: pedidos de amizade + recomendações ainda não vistas.
    val friendsState by social.friends.collectAsStateWithLifecycle()
    val recommendations by social.recommendations.collectAsStateWithLifecycle()
    val unread = friendsState.incoming.size + recommendations.count { !it.seen }
    // Reação de um amigo ao que você está ouvindo: aparece por um instante, com o app na tela.
    val reactionText = stringResource(R.string.social_reaction_toast)
    val reactionWithTitle = stringResource(R.string.social_reaction_toast_title)
    LaunchedEffect(social) {
        social.reactions.collect { reaction ->
            val text = reaction.title?.let { String.format(reactionWithTitle, reaction.from.name, reaction.emoji, it) }
                ?: String.format(reactionText, reaction.from.name, reaction.emoji)
            android.widget.Toast.makeText(context, text, android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(socialAccount?.id) {
        if (socialAccount == null) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            social.keepConnected("ui", true)
            try {
                // Dá um tempinho para a conexão abrir; se não abrir, consulta pelo HTTP.
                var wait = 5_000L
                while (true) {
                    delay(wait)
                    wait = 60_000L
                    when (social.live.value.status) {
                        LinkStatus.Online, is LinkStatus.Resting -> Unit
                        else -> social.refreshFriends()
                    }
                }
            } finally {
                social.keepConnected("ui", false)
            }
        }
    }
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
    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (isTabSwitch()) fadeIn(fadeSpec) else slideInHorizontally(slideSpec) { it }
    }
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (isTabSwitch()) fadeOut(fadeSpec)
        else slideOutHorizontally(slideSpec) { -it / 4 } + fadeOut(fadeSpec, targetAlpha = 0.4f)
    }
    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (isTabSwitch()) fadeIn(fadeSpec)
        else slideInHorizontally(slideSpec) { -it / 4 } + fadeIn(fadeSpec, initialAlpha = 0.4f)
    }
    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (isTabSwitch()) fadeOut(fadeSpec) else slideOutHorizontally(slideSpec) { it }
    }

    // App favorito para compartilhar: aparece como "Compartilhar com…" nos menus.
    val favoriteShareApp by ShareTarget.favorite(context).collectAsStateWithLifecycle()
    val favoriteShare = remember(favoriteShareApp) {
        favoriteShareApp?.let { app -> FavoriteShare(app.label) { ShareTarget.shareNextWith(app.packageName) } }
    }
    CompositionLocalProvider(
        LocalTabReselect provides reselect,
        LocalFavoriteShare provides favoriteShare,
    ) {
    Box(Modifier.fillMaxSize()) {
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
                            // A aba fica marcada também nas telas de dentro dela (ex.: um álbum da Biblioteca).
                            val selected = currentDestination?.hierarchy?.any { node ->
                                node.route == destination.route || node.route?.startsWith(destination.route + "/") == true
                            } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
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
                                },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (destination == TopLevelDestination.Profile && unread > 0) {
                                                Badge { Text(if (unread > 99) "99+" else unread.toString()) }
                                            }
                                        },
                                    ) {
                                        Icon(destination.icon, contentDescription = null)
                                    }
                                },
                                label = { Text(stringResource(destination.label)) },
                            )
                        }
                    }
                    }
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = LIBRARY_ROUTE,
                // Cada tela cuida do próprio topo; daqui só a folga das barras de baixo.
                modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
                enterTransition = enter,
                exitTransition = exit,
                popEnterTransition = popEnter,
                popExitTransition = popExit,
                // Sem isto o Android usa a animação padrão (a tela encolhe num
                // cartão e some de uma vez ao soltar o dedo).
                predictivePopEnterTransition = { popEnter(this) },
                predictivePopExitTransition = { popExit(this) },
            ) {
                val back: () -> Unit = { navController.popBackStack() }
                val openHistory: () -> Unit = { navController.navigateToHistory() }
                val openReleases: () -> Unit = { navController.navigateToReleases() }
                val openSettings: () -> Unit = { navController.navigateToSettings() }
                libraryScreen(
                    navController = navController,
                    onOpenArtist = navController::navigateToArtist,
                    onEditTags = { track -> navController.navigateToTagEditor(track.uri) },
                    onOpenReleases = openReleases,
                    onOpenSettings = openSettings,
                    onRecommend = { track -> recommendUri = track.uri },
                )
                socialScreen(
                    navController = navController,
                    onOpenArtist = navController::navigateToArtist,
                    onOpenHistory = openHistory,
                    onOpenReleases = openReleases,
                    onOpenSettings = openSettings,
                )
                artistScreen(
                    navController = navController,
                    onBack = back,
                    onOpenArtist = navController::navigateToArtist,
                )
                tageditorScreen(onBack = back)
                releasesScreen(
                    onBack = back,
                    onOpenAlbum = navController::navigateToAlbum,
                    onOpenArtist = navController::navigateToArtist,
                )
                settingsScreen(
                    navController = navController,
                    onOpenWellbeing = navController::navigateToWellbeing,
                )
                wellbeingScreen(
                    onBack = back,
                    songMenu = HistorySongMenu(
                        onOpenArtist = navController::navigateToArtist,
                        onEditTags = { uri -> navController.navigateToTagEditor(uri) },
                        onAddToPlaylist = { uri -> addToPlaylistUri = uri },
                        onRecommend = { uri -> recommendUri = uri },
                    ),
                )
            }
        }

        // Por último: fica por cima de tudo e tem prioridade no "voltar".
        PlayerSheet(
            state = playerSheet,
            // Tocar no artista dentro do player: fecha o player e abre o perfil.
            onOpenArtist = { name ->
                playerSheet.collapse()
                navController.navigateToArtist(name)
            },
            // Editar tags da música que está tocando: fecha o player e abre o editor.
            onEditTags = { uri ->
                playerSheet.collapse()
                navController.navigateToTagEditor(uri)
            },
            onAddToPlaylist = { uri -> addToPlaylistUri = uri },
            onRecommend = { uri -> recommendUri = uri },
        )

        addToPlaylistUri?.let { uri ->
            AddToPlaylistSheet(trackUris = listOf(uri), onDismiss = { addToPlaylistUri = null })
        }
        recommendUri?.let { uri ->
            RecommendSheet(trackUri = uri, onDismiss = { recommendUri = null })
        }

        // Versão nova no GitHub: aviso em tela cheia (no máximo uma consulta a cada 6 h).
        UpdatePromptHost()
    }
}
}
