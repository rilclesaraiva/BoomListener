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

/** Aba Amigos. */
const val SOCIAL_ROUTE = "social"
private const val ARG_ID = "id"
private const val CHAT_ROUTE = "social/chat?$ARG_ID={$ARG_ID}"
private const val PROFILE_ROUTE = "social/profile?$ARG_ID={$ARG_ID}"

fun NavController.navigateToChat(conversationId: String) =
    navigate("social/chat?$ARG_ID=" + Uri.encode(conversationId)) { launchSingleTop = true }

fun NavController.navigateToFriend(userId: String) =
    navigate("social/profile?$ARG_ID=" + Uri.encode(userId)) { launchSingleTop = true }

fun NavGraphBuilder.socialScreen(
    navController: NavController,
    onOpenArtist: (artistName: String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val back: () -> Unit = { navController.popBackStack() }
    composable(SOCIAL_ROUTE) {
        val context = LocalContext.current.applicationContext
        val viewModel = viewModel { SocialHomeViewModel(SocialData.repository(context)) }
        SocialHomeScreen(
            viewModel = viewModel,
            onOpenChat = navController::navigateToChat,
            onOpenProfile = navController::navigateToFriend,
            onOpenHistory = onOpenHistory,
            onOpenSettings = onOpenSettings,
        )
    }
    composable(CHAT_ROUTE, arguments = listOf(navArgument(ARG_ID) { type = NavType.StringType })) { entry ->
        val id = entry.arguments?.getString(ARG_ID).orEmpty()
        val context = LocalContext.current.applicationContext
        val viewModel = viewModel(key = "chat:$id") {
            ChatViewModel(
                conversationId = id,
                social = SocialData.repository(context),
                libraryRepository = LibraryData.repository(context),
                deezer = DeezerData.repository(context),
                player = PlayerConnection.controller(context),
            )
        }
        ChatScreen(
            viewModel = viewModel,
            onBack = back,
            onOpenProfile = navController::navigateToFriend,
            onOpenArtist = onOpenArtist,
        )
    }
    composable(PROFILE_ROUTE, arguments = listOf(navArgument(ARG_ID) { type = NavType.StringType })) { entry ->
        val id = entry.arguments?.getString(ARG_ID).orEmpty()
        val context = LocalContext.current.applicationContext
        val viewModel = viewModel(key = "friend:$id") { FriendProfileViewModel(id, SocialData.repository(context)) }
        FriendProfileScreen(viewModel = viewModel, onBack = back, onOpenChat = navController::navigateToChat)
    }
}
