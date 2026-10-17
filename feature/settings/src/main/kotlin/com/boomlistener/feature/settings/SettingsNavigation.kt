// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val SETTINGS_ROUTE = "settings"
internal const val APPEARANCE_ROUTE = "settings/appearance"
internal const val ABOUT_ROUTE = "settings/about"
internal const val PRIVACY_ROUTE = "settings/privacy"
internal const val LICENSES_ROUTE = "settings/licenses"

fun NavController.navigateToSettings() = navigate(SETTINGS_ROUTE)

/**
 * Ajustes e suas telas internas. [onOpenWellbeing] é ligado pelo :app (uma
 * feature não conhece a outra).
 */
fun NavGraphBuilder.settingsScreen(
    navController: NavController,
    onOpenWellbeing: () -> Unit,
) {
    val back: () -> Unit = { navController.popBackStack() }
    composable(SETTINGS_ROUTE) {
        SettingsScreen(
            onOpenAppearance = { navController.navigate(APPEARANCE_ROUTE) },
            onOpenWellbeing = onOpenWellbeing,
            onOpenAbout = { navController.navigate(ABOUT_ROUTE) },
            onOpenPrivacy = { navController.navigate(PRIVACY_ROUTE) },
            onOpenLicenses = { navController.navigate(LICENSES_ROUTE) },
        )
    }
    composable(APPEARANCE_ROUTE) { AppearanceScreen(onBack = back) }
    composable(ABOUT_ROUTE) { AboutScreen(onBack = back) }
    composable(PRIVACY_ROUTE) { PrivacyScreen(onBack = back) }
    composable(LICENSES_ROUTE) { LicensesScreen(onBack = back) }
}
