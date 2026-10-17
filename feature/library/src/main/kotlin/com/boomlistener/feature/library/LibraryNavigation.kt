// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.boomlistener.core.model.Track

const val LIBRARY_ROUTE = "library"

fun NavController.navigateToLibrary() = navigate(LIBRARY_ROUTE)

fun NavGraphBuilder.libraryScreen(
    onOpenArtist: (artistName: String) -> Unit,
    onEditTags: (Track) -> Unit,
) {
    composable(LIBRARY_ROUTE) {
        LibraryScreen(onOpenArtist = onOpenArtist, onEditTags = onEditTags)
    }
}
