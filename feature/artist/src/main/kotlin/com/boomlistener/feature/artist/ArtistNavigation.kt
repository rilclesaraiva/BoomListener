// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.artist

import android.net.Uri
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val ARTIST_NAME_ARG = "artistName"
const val ARTIST_ROUTE = "artist/{$ARTIST_NAME_ARG}"

// O nome vai codificado na rota ("AC/DC" tem barra, por exemplo).
fun NavController.navigateToArtist(artistName: String) = navigate("artist/${Uri.encode(artistName)}")

fun NavGraphBuilder.artistScreen(onBack: () -> Unit) {
    composable(ARTIST_ROUTE) { entry ->
        ArtistScreen(
            artistName = entry.arguments?.getString(ARTIST_NAME_ARG).orEmpty(),
            onBack = onBack,
        )
    }
}
