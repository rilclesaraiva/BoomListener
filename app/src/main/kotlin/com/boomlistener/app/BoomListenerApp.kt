// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.app

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
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.boomlistener.app.navigation.TopLevelDestination
import com.boomlistener.feature.artist.artistScreen
import com.boomlistener.feature.artist.navigateToArtist
import com.boomlistener.feature.library.LIBRARY_ROUTE
import com.boomlistener.feature.library.libraryScreen
import com.boomlistener.feature.nowplaying.navigateToNowPlaying
import com.boomlistener.feature.nowplaying.nowplayingScreen
import com.boomlistener.feature.releases.releasesScreen
import com.boomlistener.feature.settings.settingsScreen
import com.boomlistener.feature.tageditor.navigateToTagEditor
import com.boomlistener.feature.tageditor.tageditorScreen
import com.boomlistener.feature.wellbeing.wellbeingScreen

/**
 * Esqueleto do app: barra de navegação + o grafo de telas. É o ÚNICO lugar
 * que conhece todas as features e liga uma à outra (ver CLAUDE.md).
 */
@Composable
fun BoomListenerApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
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
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = LIBRARY_ROUTE,
            modifier = Modifier.padding(innerPadding),
        ) {
            libraryScreen(
                onOpenArtist = navController::navigateToArtist,
                onOpenTagEditor = navController::navigateToTagEditor,
                onOpenNowPlaying = navController::navigateToNowPlaying,
            )
            artistScreen()
            tageditorScreen()
            nowplayingScreen()
            releasesScreen()
            wellbeingScreen()
            settingsScreen()
        }
    }
}
