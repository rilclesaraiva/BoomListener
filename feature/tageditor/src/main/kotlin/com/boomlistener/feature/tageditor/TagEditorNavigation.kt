// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.tageditor

import com.boomlistener.core.common.navigateOnce
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.boomlistener.core.player.PlayerConnection
import com.boomlistener.data.deezer.DeezerData
import com.boomlistener.data.lastfm.LastfmData
import com.boomlistener.data.musicbrainz.MusicBrainzData
import com.boomlistener.data.library.LibraryData
import com.boomlistener.data.tags.TagsData

private const val ARG_URI = "uri"
const val TAGEDITOR_ROUTE = "tageditor?$ARG_URI={$ARG_URI}"

/**
 * Abre o editor de tags da música [trackUri] (endereço content:// do
 * MediaStore). Só existe um editor por vez: se já houver um aberto na pilha,
 * ele é trocado pelo novo em vez de empilhar.
 */
fun NavController.navigateToTagEditor(trackUri: String) =
    navigateOnce("tageditor?$ARG_URI=" + Uri.encode(trackUri)) {
        popUpTo(TAGEDITOR_ROUTE) { inclusive = true }
        launchSingleTop = true
    }

fun NavGraphBuilder.tageditorScreen(onBack: () -> Unit) {
    composable(
        route = TAGEDITOR_ROUTE,
        arguments = listOf(navArgument(ARG_URI) { type = NavType.StringType }),
    ) { entry ->
        val uri = entry.arguments?.getString(ARG_URI).orEmpty()
        val context = LocalContext.current.applicationContext
        val viewModel = viewModel(key = "tags:$uri") {
            TagEditorViewModel(
                uri = uri,
                tagsRepository = TagsData.repository(context),
                library = LibraryData.repository(context),
                deezer = DeezerData.repository(context),
                lastfm = LastfmData.repository(context),
                player = PlayerConnection.controller(context),
                musicBrainz = MusicBrainzData.repository(context),
            )
        }
        TagEditorScreen(viewModel = viewModel, onBack = onBack)
    }
}
