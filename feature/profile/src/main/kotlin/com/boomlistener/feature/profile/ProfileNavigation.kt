// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.profile

import com.boomlistener.core.designsystem.component.BoomNavScreen
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.boomlistener.core.player.PlayerConnection
import com.boomlistener.data.deezer.DeezerData
import com.boomlistener.data.deezer.artistPicture
import com.boomlistener.data.library.LibraryData
import com.boomlistener.data.profile.ProfileData
import com.boomlistener.data.releases.ReleasesData
import com.boomlistener.data.wellbeing.HistoryData
import com.boomlistener.data.wellbeing.StatsData

/**
 * Aba Perfil: o seu perfil, só seu (foto, capa, cor, descrição) com as suas
 * estatísticas e o histórico. Nada sai do aparelho.
 */
const val MY_PROFILE_ROUTE = "profile"

fun NavGraphBuilder.profileScreen(
    onOpenArtist: (artistName: String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    songMenu: ProfileSongMenu,
) {
    composable(MY_PROFILE_ROUTE) {
        val context = LocalContext.current.applicationContext
        val viewModel = viewModel {
            MyProfileViewModel(
                profile = ProfileData.repository(context),
                history = HistoryData.repository(context),
                stats = StatsData.repository(context),
                library = LibraryData.repository(context),
                follows = ReleasesData.follows(context),
                player = PlayerConnection.controller(context),
                remoteImage = DeezerData.repository(context)::image,
                artistPhoto = { name, sizePx -> DeezerData.repository(context).artistPicture(name, sizePx) },
            )
        }
        BoomNavScreen(this) {
            MyProfileScreen(
                viewModel = viewModel,
                onOpenArtist = onOpenArtist,
                onOpenHistory = onOpenHistory,
                onOpenSettings = onOpenSettings,
                songMenu = songMenu,
            )
        }
    }
}
