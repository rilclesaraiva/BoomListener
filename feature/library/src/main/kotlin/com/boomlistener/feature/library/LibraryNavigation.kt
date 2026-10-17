// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val LIBRARY_ROUTE = "library"

fun NavController.navigateToLibrary() = navigate(LIBRARY_ROUTE)

fun NavGraphBuilder.libraryScreen(
    onOpenArtist: (artistId: String) -> Unit,
    onOpenTagEditor: () -> Unit,
    onOpenNowPlaying: () -> Unit,
) {
    composable(LIBRARY_ROUTE) { LibraryScreen(onOpenArtist = onOpenArtist, onOpenTagEditor = onOpenTagEditor, onOpenNowPlaying = onOpenNowPlaying) }
}
