// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.app

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
import com.boomlistener.feature.artist.navigateToArtist
import com.boomlistener.feature.library.LIBRARY_ROUTE
import com.boomlistener.feature.library.libraryScreen
import com.boomlistener.feature.nowplaying.MiniPlayer
import com.boomlistener.feature.nowplaying.PlayerSheet
import com.boomlistener.feature.nowplaying.rememberPlayerSheetState
import com.boomlistener.feature.releases.releasesScreen
import com.boomlistener.feature.settings.settingsScreen
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

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                Column {
                    MiniPlayer(sheetState = playerSheet)
                    NavigationBar {
                        TopLevelDestination.entries.forEach { destination ->
                            // A aba fica marcada também nas telas de dentro dela (ex.: um álbum da Biblioteca).
                            val selected = currentDestination?.hierarchy?.any { node ->
                                node.route == destination.route || node.route?.startsWith(destination.route + "/") == true
                            } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(destination.icon, contentDescription = null) },
                                label = { Text(stringResource(destination.label)) },
                            )
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
                val openSettings: () -> Unit = { navController.navigateToSettings() }
                libraryScreen(
                    navController = navController,
                    onOpenArtist = navController::navigateToArtist,
                    onEditTags = { track -> navController.navigateToTagEditor(track.uri) },
                    onOpenHistory = openHistory,
                    onOpenSettings = openSettings,
                )
                artistScreen(
                    navController = navController,
                    onBack = back,
                    onOpenArtist = navController::navigateToArtist,
                )
                tageditorScreen(onBack = back)
                releasesScreen(onOpenHistory = openHistory, onOpenSettings = openSettings)
                settingsScreen(
                    navController = navController,
                    onOpenWellbeing = navController::navigateToWellbeing,
                )
                wellbeingScreen(onBack = back)
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
        )

        addToPlaylistUri?.let { uri ->
            AddToPlaylistSheet(trackUris = listOf(uri), onDismiss = { addToPlaylistUri = null })
        }
    }
}
