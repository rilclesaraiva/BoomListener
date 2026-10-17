// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.wellbeing

import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.boomlistener.core.player.PlayerConnection
import com.boomlistener.data.library.LibraryData
import com.boomlistener.data.wellbeing.HistoryData
import java.time.temporal.WeekFields
import java.util.Locale

const val WELLBEING_ROUTE = "wellbeing"
const val HISTORY_ROUTE = "history"

fun NavController.navigateToWellbeing() = navigate(WELLBEING_ROUTE)

fun NavController.navigateToHistory() = navigate(HISTORY_ROUTE) { launchSingleTop = true }

fun NavGraphBuilder.wellbeingScreen(onBack: () -> Unit) {
    composable(WELLBEING_ROUTE) { WellbeingScreen(onBack = onBack) }
    composable(HISTORY_ROUTE) {
        val context = LocalContext.current.applicationContext
        val viewModel = viewModel {
            HistoryViewModel(
                history = HistoryData.repository(context),
                library = LibraryData.repository(context),
                player = PlayerConnection.controller(context),
                // A semana começa no dia do costume local (domingo no Brasil).
                firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek,
            )
        }
        HistoryScreen(viewModel = viewModel, onBack = onBack)
    }
}
