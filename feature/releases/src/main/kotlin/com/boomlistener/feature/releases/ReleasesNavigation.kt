// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.releases

import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.boomlistener.data.deezer.DeezerData
import com.boomlistener.data.releases.ReleasesData

const val RELEASES_ROUTE = "releases"

fun NavController.navigateToReleases() = navigate(RELEASES_ROUTE)

fun NavGraphBuilder.releasesScreen(
    onOpenAlbum: (albumId: Long, artistName: String) -> Unit,
    onOpenArtist: (artistName: String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    composable(RELEASES_ROUTE) {
        val context = LocalContext.current.applicationContext
        val viewModel = viewModel { ReleasesViewModel(ReleasesData.follows(context), DeezerData.repository(context)) }
        ReleasesScreen(
            viewModel = viewModel,
            onOpenAlbum = onOpenAlbum,
            onOpenArtist = onOpenArtist,
            onOpenHistory = onOpenHistory,
            onOpenSettings = onOpenSettings,
        )
    }
}
