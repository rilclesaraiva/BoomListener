// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.boomlistener.core.model.Track
import com.boomlistener.core.player.PlayerConnection
import com.boomlistener.data.deezer.DeezerData
import com.boomlistener.data.library.LibraryData
import com.boomlistener.data.library.PlaylistData

/** Aba Início: a lista de músicas. */
const val LIBRARY_ROUTE = "library"

/** Aba Biblioteca: gêneros, artistas, playlists e álbuns. */
const val COLLECTIONS_ROUTE = "collections"

private const val ARG_KEY = "key"
private const val GENRE_ROUTE = "collections/genre?$ARG_KEY={$ARG_KEY}"
private const val ALBUM_ROUTE = "collections/album?$ARG_KEY={$ARG_KEY}"
private const val PLAYLIST_ROUTE = "collections/playlist?$ARG_KEY={$ARG_KEY}"

fun NavController.navigateToLibrary() = navigate(LIBRARY_ROUTE)

private fun NavController.openCollection(base: String, key: String) =
    navigate("collections/$base?$ARG_KEY=" + Uri.encode(key))

fun NavGraphBuilder.libraryScreen(
    navController: NavController,
    onOpenArtist: (artistName: String) -> Unit,
    onEditTags: (Track) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onRecommend: (Track) -> Unit,
) {
    composable(LIBRARY_ROUTE) {
        LibraryScreen(
            onOpenArtist = onOpenArtist,
            onOpenAlbum = { navController.openCollection("album", it) },
            onEditTags = onEditTags,
            onRecommend = onRecommend,
            onOpenHistory = onOpenHistory,
            onOpenSettings = onOpenSettings,
        )
    }

    composable(COLLECTIONS_ROUTE) {
        val context = LocalContext.current.applicationContext
        val viewModel = viewModel {
            CollectionsViewModel(LibraryData.repository(context), PlaylistData.repository(context))
        }
        CollectionsScreen(
            viewModel = viewModel,
            onOpenGenre = { navController.openCollection("genre", it) },
            onOpenArtist = onOpenArtist,
            onOpenAlbum = { navController.openCollection("album", it) },
            onOpenPlaylist = { navController.openCollection("playlist", it.toString()) },
            onOpenHistory = onOpenHistory,
            onOpenSettings = onOpenSettings,
        )
    }

    val back: () -> Unit = { navController.popBackStack() }
    listOf(GENRE_ROUTE, ALBUM_ROUTE, PLAYLIST_ROUTE).forEach { route ->
        composable(route, arguments = listOf(navArgument(ARG_KEY) { type = NavType.StringType })) { entry ->
            val key = entry.arguments?.getString(ARG_KEY).orEmpty()
            val ref = when (route) {
                GENRE_ROUTE -> CollectionRef.Genre(key)
                ALBUM_ROUTE -> CollectionRef.Album(key)
                else -> CollectionRef.Playlist(key.toLongOrNull() ?: -1)
            }
            val context = LocalContext.current.applicationContext
            val viewModel = viewModel(key = "$route:$key") {
                CollectionDetailViewModel(
                    ref = ref,
                    library = LibraryData.repository(context),
                    playlists = PlaylistData.repository(context),
                    player = PlayerConnection.controller(context),
                    songLink = DeezerData.repository(context)::youtubeMusicLinkForSong,
                )
            }
            CollectionDetailScreen(
                viewModel = viewModel,
                onBack = back,
                onOpenArtist = onOpenArtist,
                onEditTags = onEditTags,
                onRecommend = onRecommend,
            )
        }
    }
}
