// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.social

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
import com.boomlistener.data.library.LibraryData
import com.boomlistener.data.social.SocialData
import com.boomlistener.data.releases.ReleasesData
import com.boomlistener.data.wellbeing.HistoryData
import com.boomlistener.data.wellbeing.StatsData

private const val ARG_ID = "id"
private const val PROFILE_ROUTE = "social/profile?$ARG_ID={$ARG_ID}"

/**
 * Aba Perfil: o seu perfil (estatísticas, histórico, redes) com os amigos
 * dentro dele: o que estão ouvindo, pedidos e recomendações.
 */
const val MY_PROFILE_ROUTE = "social/me"

fun NavController.navigateToFriend(userId: String) =
    navigate("social/profile?$ARG_ID=" + Uri.encode(userId)) { launchSingleTop = true }

fun NavGraphBuilder.socialScreen(
    navController: NavController,
    onOpenArtist: (artistName: String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenReleases: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val back: () -> Unit = { navController.popBackStack() }
    composable(PROFILE_ROUTE, arguments = listOf(navArgument(ARG_ID) { type = NavType.StringType })) { entry ->
        val id = entry.arguments?.getString(ARG_ID).orEmpty()
        val context = LocalContext.current.applicationContext
        val viewModel = viewModel(key = "friend:$id") { FriendProfileViewModel(id, SocialData.repository(context)) }
        FriendProfileScreen(viewModel = viewModel, onBack = back)
    }
    composable(MY_PROFILE_ROUTE) {
        val context = LocalContext.current.applicationContext
        val viewModel = viewModel {
            MyProfileViewModel(
                social = SocialData.repository(context),
                history = HistoryData.repository(context),
                stats = StatsData.repository(context),
                library = LibraryData.repository(context),
                follows = ReleasesData.follows(context),
                player = PlayerConnection.controller(context),
                remoteImage = DeezerData.repository(context)::image,
            )
        }
        val friends = viewModel {
            val library = LibraryData.repository(context)
            SocialHomeViewModel(
                social = SocialData.repository(context),
                library = library::getTracks,
                player = PlayerConnection.controller(context),
                remoteImage = DeezerData.repository(context)::image,
            )
        }
        MyProfileScreen(
            viewModel = viewModel,
            friendsViewModel = friends,
            onOpenFriend = { navController.navigateToFriend(it) },
            onOpenArtist = onOpenArtist,
            onOpenHistory = onOpenHistory,
            onOpenReleases = onOpenReleases,
            onOpenSettings = onOpenSettings,
        )
    }
}
