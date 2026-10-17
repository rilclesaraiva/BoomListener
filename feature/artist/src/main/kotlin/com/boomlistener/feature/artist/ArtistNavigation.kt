// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/boomlistener/blob/main/LICENSE

package com.boomlistener.feature.artist

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val ARTIST_ID_ARG = "artistId"
const val ARTIST_ROUTE = "artist/{$ARTIST_ID_ARG}"

fun NavController.navigateToArtist(artistId: String) = navigate("artist/$artistId")

fun NavGraphBuilder.artistScreen() {
    composable(ARTIST_ROUTE) { entry ->
        ArtistScreen(artistId = entry.arguments?.getString(ARTIST_ID_ARG).orEmpty())
    }
}
