// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.wellbeing

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val WELLBEING_ROUTE = "wellbeing"

fun NavController.navigateToWellbeing() = navigate(WELLBEING_ROUTE)

fun NavGraphBuilder.wellbeingScreen(onBack: () -> Unit) {
    composable(WELLBEING_ROUTE) { WellbeingScreen(onBack = onBack) }
}
