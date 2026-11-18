// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.profile

import com.boomlistener.core.common.navigateOnce
import com.boomlistener.core.designsystem.component.BoomNavScreen
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.boomlistener.core.player.PlayerConnection
import com.boomlistener.data.deezer.DeezerData
import com.boomlistener.data.deezer.artistLatestCover
import com.boomlistener.data.deezer.artistPicture
import com.boomlistener.data.library.LibraryData
import com.boomlistener.data.library.PlaylistData
import com.boomlistener.data.profile.ProfileData
import com.boomlistener.data.releases.ReleasesData
import com.boomlistener.data.wellbeing.DailyStatsData
import com.boomlistener.data.wellbeing.HistoryData
import com.boomlistener.data.wellbeing.StatsData
import com.boomlistener.data.wellbeing.StatsPeriod
import com.boomlistener.data.wellbeing.WrappedAlertsData

/**
 * Aba Perfil: o seu perfil, só seu (foto, capa, cor, descrição) com as suas
 * estatísticas e o histórico. Nada sai do aparelho.
 */
const val MY_PROFILE_ROUTE = "profile"

fun NavGraphBuilder.profileScreen(
    onOpenArtist: (artistName: String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenWrapped: (period: String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    composable(MY_PROFILE_ROUTE) {
        val context = LocalContext.current.applicationContext
        val viewModel = viewModel { createProfileViewModel(context) }
        BoomNavScreen(this, tab = true) {
            MyProfileScreen(
                viewModel = viewModel,
                onOpenArtist = onOpenArtist,
                onOpenHistory = onOpenHistory,
                onOpenStats = onOpenStats,
                onOpenWrapped = onOpenWrapped,
                onOpenSettings = onOpenSettings,
            )
        }
    }
}

/** Tela de Estatísticas: o resumo do Perfil abre esta página (com o voltar). */
const val STATS_ROUTE = "profile/stats"

fun NavController.navigateToStats() = navigateOnce(STATS_ROUTE) { launchSingleTop = true }

fun NavGraphBuilder.statsScreen(
    onOpenWrapped: (period: String) -> Unit,
    onBack: () -> Unit,
    onOpenArtist: (artistName: String) -> Unit,
    songMenu: ProfileSongMenu,
) {
    composable(STATS_ROUTE) {
        val context = LocalContext.current.applicationContext
        val viewModel = viewModel { createProfileViewModel(context) }
        BoomNavScreen(this) {
            StatsScreen(viewModel = viewModel, onBack = onBack, onOpenWrapped = onOpenWrapped, onOpenArtist = onOpenArtist, songMenu = songMenu)
        }
    }
}

/** Wrapped: cartões em sequência com o período escolhido nas Estatísticas. */
const val WRAPPED_ROUTE = "profile/wrapped"

fun NavController.navigateToWrapped(period: String) = navigateOnce("$WRAPPED_ROUTE/$period") { launchSingleTop = true }

fun NavGraphBuilder.wrappedScreen(onBack: () -> Unit) {
    composable("$WRAPPED_ROUTE/{period}") { entry ->
        val context = LocalContext.current.applicationContext
        val viewModel = viewModel { createProfileViewModel(context) }
        val period = entry.arguments?.getString("period")?.let { name -> StatsPeriod.entries.firstOrNull { it.name == name } } ?: StatsPeriod.ThisYear
        BoomNavScreen(this) {
            WrappedScreen(viewModel = viewModel, period = period, onBack = onBack)
        }
    }
}

private fun createProfileViewModel(context: android.content.Context) = MyProfileViewModel(
    profile = ProfileData.repository(context),
    history = HistoryData.repository(context),
    stats = StatsData.repository(context),
    library = LibraryData.repository(context),
    follows = ReleasesData.follows(context),
    player = PlayerConnection.controller(context),
    remoteImage = DeezerData.repository(context)::image,
    artistPhoto = { name, sizePx -> DeezerData.repository(context).artistPicture(name, sizePx) },
    artistLatestCover = { name, sizePx -> DeezerData.repository(context).artistLatestCover(name, sizePx) },
    daily = DailyStatsData.repository(context),
    playlists = PlaylistData.repository(context),
    wrappedAlerts = WrappedAlertsData.repository(context),
)
