// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
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
import com.boomlistener.core.designsystem.component.BoomPageTitle
import com.boomlistener.core.designsystem.component.BoomScreenScaffold
import com.boomlistener.core.designsystem.component.BoomSettingsItem
import com.boomlistener.core.designsystem.component.BoomSettingsSection

@Composable
internal fun SettingsScreen(
    onOpenAppearance: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenPlayback: () -> Unit,
    onOpenShare: () -> Unit,
    onOpenSystem: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val version = appVersionName(context)
    BoomScreenScaffold(
        title = stringResource(R.string.feature_settings_title),
        onBack = onBack,
        modifier = modifier,
        // Sem barra presa na tela: o título é o topo da página.
        topBar = {},
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            item(key = "pageTitle") {
                BoomPageTitle(stringResource(R.string.feature_settings_title), hasBack = true, large = true)
            }
            item {
                // Ordem: como o app fica, como toca, o que toca, para onde vai,
                // seus hábitos e, por último, o técnico (internet, segundo plano, backup).
                BoomSettingsSection(title = stringResource(R.string.feature_settings_section_general)) {
                    BoomSettingsItem(
                        icon = BoomIcons.Palette,
                        accent = BoomAccent.Primary,
                        title = stringResource(R.string.feature_settings_appearance),
                        subtitle = stringResource(R.string.feature_settings_appearance_summary),
                        onClick = onOpenAppearance,
                    )
                    BoomSettingsItem(
                        icon = BoomIcons.MusicNote,
                        accent = BoomAccent.Tertiary,
                        title = stringResource(R.string.feature_settings_playback),
                        subtitle = stringResource(R.string.feature_settings_playback_summary),
                        onClick = onOpenPlayback,
                    )
                    BoomSettingsItem(
                        icon = BoomIcons.Collection,
                        accent = BoomAccent.Secondary,
                        title = stringResource(R.string.feature_settings_library),
                        subtitle = stringResource(R.string.feature_settings_library_summary),
                        onClick = onOpenLibrary,
                    )
                    BoomSettingsItem(
                        icon = BoomIcons.Send,
                        accent = BoomAccent.Primary,
                        title = stringResource(R.string.feature_settings_share),
                        subtitle = stringResource(R.string.feature_settings_share_summary),
                        onClick = onOpenShare,
                    )
                    // Sistema: internet, segundo plano e backup.
                    BoomSettingsItem(
                        icon = BoomIcons.Settings,
                        accent = BoomAccent.Secondary,
                        title = stringResource(R.string.feature_settings_system),
                        subtitle = stringResource(R.string.feature_settings_system_summary),
                        onClick = onOpenSystem,
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
        .getOrNull()?.removeSuffix("-debug") ?: "?"
