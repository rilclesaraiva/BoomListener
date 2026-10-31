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
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.boomlistener.data.updates.UpdatePromptState
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
import com.boomlistener.core.designsystem.component.BoomPageTitle
import com.boomlistener.core.designsystem.component.BoomScreenScaffold
import com.boomlistener.core.designsystem.component.BoomSettingsItem
import com.boomlistener.core.designsystem.component.BoomSettingsSection

@Composable
internal fun SettingsScreen(
    onOpenAppearance: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenLicenses: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenPlayback: () -> Unit,
    onOpenShare: () -> Unit,
    onOpenSystem: () -> Unit,
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
    val prompts by updatesViewModel.prompts.collectAsStateWithLifecycle()
    var choosingPrompts by remember { mutableStateOf(false) }
    if (choosingPrompts) {
        UpdatePromptsDialog(
            onAlways = { updatesViewModel.resumePrompts(); choosingPrompts = false },
            onSnooze = { period -> updatesViewModel.snooze(period); choosingPrompts = false },
            onDismiss = { choosingPrompts = false },
        )
    }

    val available = (updatesState as? UpdatesUiState.Done)?.result as? UpdateCheckResult.Available
    if (showUpdateDialog && available != null) {
        UpdatePromptDialog(
            update = available,
            onDownload = {
                updatesViewModel.dismissDialog()
                context.openUrl(available.downloadUrl ?: available.pageUrl)
            },
            onLater = updatesViewModel::dismissDialog,
            onSnooze = updatesViewModel::snooze,
            onSkip = updatesViewModel::skipVersion,
        )
    }
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
                    // Avisos automáticos de versão nova: ligados, adiados ou pulando
                    // uma versão. Dá para desfazer aqui o que foi escolhido no aviso.
                    BoomSettingsItem(
                        icon = BoomIcons.NotificationsOff,
                        title = stringResource(R.string.feature_settings_update_prompts),
                        subtitle = updatePromptsSubtitle(prompts),
                        onClick = { choosingPrompts = true },
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

@Composable
private fun updatePromptsSubtitle(state: UpdatePromptState): String {
    val now = System.currentTimeMillis()
    val skipped = state.skippedVersion
    return when {
        state.snoozedUntilMs > now -> stringResource(
            R.string.feature_settings_update_prompts_snoozed,
            java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(java.util.Date(state.snoozedUntilMs)),
        )
        skipped != null -> stringResource(R.string.feature_settings_update_prompts_skipped, skipped.removePrefix("v"))
        else -> stringResource(R.string.feature_settings_update_prompts_on)
    }
}

/** Escolher como ficam os avisos de versão nova: normais, ou pausados por um tempo. */
@Composable
private fun UpdatePromptsDialog(onAlways: () -> Unit, onSnooze: (SnoozePeriod) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.feature_settings_update_prompts)) },
        text = {
            Column {
                TextButton(onClick = onAlways) { Text(stringResource(R.string.feature_settings_update_prompts_always)) }
                TextButton(onClick = { onSnooze(SnoozePeriod.Week) }) { Text(stringResource(R.string.feature_settings_update_prompt_week)) }
                TextButton(onClick = { onSnooze(SnoozePeriod.Month) }) { Text(stringResource(R.string.feature_settings_update_prompt_month)) }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_settings_backup_cancel)) } },
    )
}

/** Versão instalada (vem do APP_VERSION, via versionName). */
@Suppress("DEPRECATION")
internal fun appVersionName(context: Context): String =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
        .getOrNull()?.removeSuffix("-debug") ?: "?"
