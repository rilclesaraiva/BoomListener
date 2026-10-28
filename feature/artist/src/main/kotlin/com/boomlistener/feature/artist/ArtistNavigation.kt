// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.artist

import com.boomlistener.core.designsystem.component.BoomNavScreen
import com.boomlistener.core.common.navigateOnce
import android.net.Uri
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.composable

const val ARTIST_NAME_ARG = "artistName"
const val ARTIST_ROUTE = "artist/{$ARTIST_NAME_ARG}"

// O nome vai codificado na rota ("AC/DC" tem barra, por exemplo).
fun NavController.navigateToArtist(artistName: String) = navigateOnce("artist/${Uri.encode(artistName)}")

private const val ALBUM_ID_ARG = "albumId"
private const val ALBUM_ROUTE = "album/{$ALBUM_ID_ARG}?$ARTIST_NAME_ARG={$ARTIST_NAME_ARG}"

/** Abre um álbum do Deezer; [artistName] serve para achar as suas músicas dele. */
fun NavController.navigateToAlbum(albumId: Long, artistName: String) =
    navigateOnce("album/$albumId?$ARTIST_NAME_ARG=${Uri.encode(artistName)}")

/** Perfil do artista e as telas de álbum que se abrem a partir dele. */
fun NavGraphBuilder.artistScreen(
    navController: NavController,
    onBack: () -> Unit,
    onOpenArtist: (artistName: String) -> Unit,
) {
    composable(ARTIST_ROUTE) { entry ->
        val name = entry.arguments?.getString(ARTIST_NAME_ARG).orEmpty()
        BoomNavScreen(this) {
            ArtistScreen(
                artistName = name,
                onBack = onBack,
                onOpenArtist = onOpenArtist,
                onOpenAlbum = { albumId -> navController.navigateToAlbum(albumId, name) },
            )
        }
    }
    composable(
        route = ALBUM_ROUTE,
        arguments = listOf(
            navArgument(ALBUM_ID_ARG) { type = NavType.LongType },
            navArgument(ARTIST_NAME_ARG) {
                type = NavType.StringType
                defaultValue = ""
            },
        ),
    ) { entry ->
        BoomNavScreen(this) {
            AlbumScreen(
                albumId = entry.arguments?.getLong(ALBUM_ID_ARG) ?: 0L,
                artistName = entry.arguments?.getString(ARTIST_NAME_ARG).orEmpty(),
                onBack = onBack,
            )
        }
    }
}
