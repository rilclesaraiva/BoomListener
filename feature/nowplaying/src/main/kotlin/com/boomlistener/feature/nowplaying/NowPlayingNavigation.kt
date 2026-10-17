// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/boomlistener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val NOWPLAYING_ROUTE = "nowplaying"

fun NavController.navigateToNowPlaying() = navigate(NOWPLAYING_ROUTE)

fun NavGraphBuilder.nowplayingScreen(
) {
    composable(NOWPLAYING_ROUTE) { NowPlayingScreen() }
}
