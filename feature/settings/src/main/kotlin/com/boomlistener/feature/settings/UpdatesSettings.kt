// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.boomlistener.core.designsystem.component.BoomAccent
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.designsystem.component.BoomSettingsItem
import com.boomlistener.core.designsystem.component.BoomSettingsSection
import com.boomlistener.data.updates.UpdateCheckResult
import com.boomlistener.data.updates.UpdatePromptState
import com.boomlistener.data.updates.UpdatesData

/**
 * Ajustes → Sistema → Atualizações: verificar se há versão nova e escolher como
 * ficam os avisos automáticos. Também mostra o aviso de versão nova quando a
 * verificação acha uma.
 */
@Composable
internal fun UpdatesSettingsSection() {
    val context = LocalContext.current
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

    BoomSettingsSection(title = stringResource(R.string.feature_settings_system_updates)) {
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

