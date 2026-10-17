// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val NOWPLAYING_ROUTE = "nowplaying"

fun NavController.navigateToNowPlaying() = navigate(NOWPLAYING_ROUTE) { launchSingleTop = true }

fun NavGraphBuilder.nowplayingScreen(onBack: () -> Unit) {
    composable(NOWPLAYING_ROUTE) { NowPlayingScreen(onBack = onBack) }
}
