// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/boomlistener/blob/main/LICENSE

package com.boomlistener.feature.releases

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val RELEASES_ROUTE = "releases"

fun NavController.navigateToReleases() = navigate(RELEASES_ROUTE)

fun NavGraphBuilder.releasesScreen(
) {
    composable(RELEASES_ROUTE) { ReleasesScreen() }
}
