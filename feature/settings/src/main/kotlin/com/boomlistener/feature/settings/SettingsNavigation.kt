// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.boomlistener.data.social.SocialData

const val SETTINGS_ROUTE = "settings"
internal const val APPEARANCE_ROUTE = "settings/appearance"
internal const val ABOUT_ROUTE = "settings/about"
internal const val PRIVACY_ROUTE = "settings/privacy"
internal const val LICENSES_ROUTE = "settings/licenses"
internal const val FRIENDS_ROUTE = "settings/friends"

fun NavController.navigateToSettings() = navigate(SETTINGS_ROUTE) { launchSingleTop = true }

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
            onOpenFriends = { navController.navigate(FRIENDS_ROUTE) },
            onBack = back,
        )
    }
    composable(APPEARANCE_ROUTE) { AppearanceScreen(onBack = back) }
    composable(ABOUT_ROUTE) { AboutScreen(onBack = back) }
    composable(PRIVACY_ROUTE) { PrivacyScreen(onBack = back) }
    composable(LICENSES_ROUTE) { LicensesScreen(onBack = back) }
    composable(FRIENDS_ROUTE) {
        val context = LocalContext.current.applicationContext
        val viewModel = viewModel { FriendsSettingsViewModel(SocialData.repository(context)) }
        FriendsSettingsScreen(viewModel = viewModel, onBack = back)
    }
}
