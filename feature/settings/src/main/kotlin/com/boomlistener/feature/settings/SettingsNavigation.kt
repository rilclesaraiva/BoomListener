// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.settings

import com.boomlistener.core.designsystem.component.BoomNavScreen
import com.boomlistener.core.common.popBackStackOnce
import com.boomlistener.core.common.navigateOnce
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val SETTINGS_ROUTE = "settings"
internal const val APPEARANCE_ROUTE = "settings/appearance"
internal const val ABOUT_ROUTE = "settings/about"
internal const val PRIVACY_ROUTE = "settings/privacy"
internal const val LICENSES_ROUTE = "settings/licenses"
internal const val LIBRARY_SETTINGS_ROUTE = "settings/library"
internal const val PLAYBACK_SETTINGS_ROUTE = "settings/playback"
internal const val SHARE_SETTINGS_ROUTE = "settings/share"
internal const val BACKUP_ROUTE = "settings/backup"
internal const val SYSTEM_SETTINGS_ROUTE = "settings/system"

fun NavController.navigateToSettings() = navigateOnce(SETTINGS_ROUTE) { launchSingleTop = true }

/** Ajustes e suas telas internas. */
fun NavGraphBuilder.settingsScreen(navController: NavController) {
    val back: () -> Unit = { navController.popBackStackOnce() }
    composable(SETTINGS_ROUTE) {
        BoomNavScreen(this) {
            SettingsScreen(
                onOpenAppearance = { navController.navigateOnce(APPEARANCE_ROUTE) },
                onOpenAbout = { navController.navigateOnce(ABOUT_ROUTE) },
                onOpenPrivacy = { navController.navigateOnce(PRIVACY_ROUTE) },
                onOpenLicenses = { navController.navigateOnce(LICENSES_ROUTE) },
                onOpenLibrary = { navController.navigateOnce(LIBRARY_SETTINGS_ROUTE) },
                onOpenPlayback = { navController.navigateOnce(PLAYBACK_SETTINGS_ROUTE) },
                onOpenShare = { navController.navigateOnce(SHARE_SETTINGS_ROUTE) },
                onOpenSystem = { navController.navigateOnce(SYSTEM_SETTINGS_ROUTE) },
                onBack = back,
            )
        }
    }
    composable(APPEARANCE_ROUTE) { BoomNavScreen(this) { AppearanceScreen(onBack = back) } }
    composable(LIBRARY_SETTINGS_ROUTE) { BoomNavScreen(this) { LibrarySettingsScreen(onBack = back) } }
    composable(PLAYBACK_SETTINGS_ROUTE) { BoomNavScreen(this) { PlaybackSettingsScreen(onBack = back) } }
    composable(SHARE_SETTINGS_ROUTE) { BoomNavScreen(this) { ShareSettingsScreen(onBack = back) } }
    composable(BACKUP_ROUTE) { BoomNavScreen(this) { BackupScreen(onBack = back) } }
    composable(SYSTEM_SETTINGS_ROUTE) { BoomNavScreen(this) {
            SystemSettingsScreen(onBack = back, onOpenBackup = { navController.navigateOnce(BACKUP_ROUTE) })
        } }
    composable(ABOUT_ROUTE) { BoomNavScreen(this) { AboutScreen(onBack = back) } }
    composable(PRIVACY_ROUTE) { BoomNavScreen(this) { PrivacyScreen(onBack = back) } }
    composable(LICENSES_ROUTE) { BoomNavScreen(this) { LicensesScreen(onBack = back) } }
}
