// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.settings

import android.content.Context
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.boomlistener.core.designsystem.component.BoomAccent
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.designsystem.component.BoomScreenScaffold
import com.boomlistener.core.designsystem.component.BoomSettingsItem
import com.boomlistener.core.designsystem.component.BoomSettingsSection

@Composable
internal fun SettingsScreen(
    onOpenAppearance: () -> Unit,
    onOpenWellbeing: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenLicenses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val version = appVersionName(LocalContext.current)
    BoomScreenScaffold(
        title = stringResource(R.string.feature_settings_title),
        largeTitle = true,
        modifier = modifier,
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                BoomSettingsSection(title = stringResource(R.string.feature_settings_section_general)) {
                    BoomSettingsItem(
                        icon = BoomIcons.Palette,
                        accent = BoomAccent.Primary,
                        title = stringResource(R.string.feature_settings_appearance),
                        subtitle = stringResource(R.string.feature_settings_appearance_summary),
                        onClick = onOpenAppearance,
                    )
                    BoomSettingsItem(
                        icon = BoomIcons.Wellbeing,
                        accent = BoomAccent.Tertiary,
                        title = stringResource(R.string.feature_settings_wellbeing),
                        subtitle = stringResource(R.string.feature_settings_wellbeing_summary),
                        onClick = onOpenWellbeing,
                    )
                }
            }
            item {
                BoomSettingsSection(title = stringResource(R.string.feature_settings_section_about)) {
                    BoomSettingsItem(
                        icon = BoomIcons.Info,
                        accent = BoomAccent.Secondary,
                        title = stringResource(R.string.feature_settings_about),
                        subtitle = stringResource(R.string.feature_settings_about_summary, version),
                        onClick = onOpenAbout,
                    )
                    BoomSettingsItem(
                        icon = BoomIcons.Privacy,
                        title = stringResource(R.string.feature_settings_privacy),
                        subtitle = stringResource(R.string.feature_settings_privacy_summary),
                        onClick = onOpenPrivacy,
                    )
                    BoomSettingsItem(
                        icon = BoomIcons.Article,
                        title = stringResource(R.string.feature_settings_licenses),
                        subtitle = stringResource(R.string.feature_settings_licenses_summary),
                        onClick = onOpenLicenses,
                    )
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

/** Versão instalada (vem do APP_VERSION, via versionName). */
@Suppress("DEPRECATION")
internal fun appVersionName(context: Context): String =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
        .getOrNull() ?: "?"
