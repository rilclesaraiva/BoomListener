// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val SETTINGS_ROUTE = "settings"

fun NavController.navigateToSettings() = navigate(SETTINGS_ROUTE)

/** [onOpenWellbeing] é ligado pelo :app (uma feature não conhece a outra). */
fun NavGraphBuilder.settingsScreen(onOpenWellbeing: () -> Unit) {
    composable(SETTINGS_ROUTE) { SettingsScreen(onOpenWellbeing = onOpenWellbeing) }
}
