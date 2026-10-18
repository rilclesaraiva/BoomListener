// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.settings

import android.content.Context
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.boomlistener.data.updates.UpdateCheckResult
import com.boomlistener.data.updates.UpdatesData
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
    onOpenFriends: () -> Unit,
    onOpenLibrary: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val version = appVersionName(context)
    val updatesViewModel: UpdatesViewModel = viewModel {
        UpdatesViewModel(UpdatesData.repository(context.applicationContext))
    }
    val updatesState by updatesViewModel.state.collectAsStateWithLifecycle()
    val showUpdateDialog by updatesViewModel.showDialog.collectAsStateWithLifecycle()

    val available = (updatesState as? UpdatesUiState.Done)?.result as? UpdateCheckResult.Available
    if (showUpdateDialog && available != null) {
        AlertDialog(
            onDismissRequest = updatesViewModel::dismissDialog,
            title = { Text(stringResource(R.string.feature_settings_updates_dialog_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.feature_settings_updates_dialog_text,
                        available.latestVersion,
                        available.currentVersion,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    updatesViewModel.dismissDialog()
                    context.openUrl(available.downloadUrl ?: available.pageUrl)
                }) { Text(stringResource(R.string.feature_settings_updates_download)) }
            },
            dismissButton = {
                TextButton(onClick = updatesViewModel::dismissDialog) {
                    Text(stringResource(R.string.feature_settings_updates_later))
                }
            },
        )
    }
    BoomScreenScaffold(
        title = stringResource(R.string.feature_settings_title),
        onBack = onBack,
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
                        icon = BoomIcons.Collection,
                        accent = BoomAccent.Secondary,
                        title = stringResource(R.string.feature_settings_library),
                        subtitle = stringResource(R.string.feature_settings_library_summary),
                        onClick = onOpenLibrary,
                    )
                    BoomSettingsItem(
                        icon = BoomIcons.Wellbeing,
                        accent = BoomAccent.Tertiary,
                        title = stringResource(R.string.feature_settings_wellbeing),
                        subtitle = stringResource(R.string.feature_settings_wellbeing_summary),
                        onClick = onOpenWellbeing,
                    )
                    BoomSettingsItem(
                        icon = BoomIcons.Chat,
                        accent = BoomAccent.Secondary,
                        title = stringResource(R.string.feature_settings_friends),
                        subtitle = stringResource(R.string.feature_settings_friends_summary),
                        onClick = onOpenFriends,
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
                        icon = BoomIcons.Update,
                        accent = BoomAccent.Primary,
                        title = stringResource(R.string.feature_settings_updates),
                        subtitle = updatesSubtitle(updatesState),
                        onClick = {
                            if (available != null) context.openUrl(available.downloadUrl ?: available.pageUrl)
                            else updatesViewModel.check()
                        },
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

@Composable
private fun updatesSubtitle(state: UpdatesUiState): String = when (state) {
    UpdatesUiState.Idle -> stringResource(R.string.feature_settings_updates_summary)
    UpdatesUiState.Checking -> stringResource(R.string.feature_settings_updates_checking)
    is UpdatesUiState.Done -> when (val result = state.result) {
        is UpdateCheckResult.UpToDate ->
            stringResource(R.string.feature_settings_updates_up_to_date, result.currentVersion)
        is UpdateCheckResult.Available ->
            stringResource(R.string.feature_settings_updates_available, result.latestVersion)
        UpdateCheckResult.NoReleases -> stringResource(R.string.feature_settings_updates_no_releases)
        UpdateCheckResult.Failed -> stringResource(R.string.feature_settings_updates_failed)
    }
}

/** Versão instalada (vem do APP_VERSION, via versionName). */
@Suppress("DEPRECATION")
internal fun appVersionName(context: Context): String =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
        .getOrNull()?.removeSuffix("-debug") ?: "?"
