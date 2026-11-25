// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.boomlistener.core.designsystem.component.BoomModalSheet
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import com.boomlistener.core.designsystem.theme.boomSpatialSpec
import com.boomlistener.core.designsystem.theme.boomEffectsSpec
import androidx.compose.ui.draw.rotate
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Icon
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
            accent = BoomAccent.Teal,
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
            accent = BoomAccent.Teal,
            title = stringResource(R.string.feature_settings_update_prompts),
            subtitle = updatePromptsSubtitle(prompts),
            onClick = { choosingPrompts = !choosingPrompts },
            trailing = {
                val turn by animateFloatAsState(if (choosingPrompts) 180f else 0f, boomSpatialSpec(), label = "promptsArrow")
                Icon(BoomIcons.Collapse, contentDescription = null, modifier = Modifier.rotate(turn))
            },
        )
        // Recolhível: as escolhas abrem aqui mesmo, sem painel.
        AnimatedVisibility(
            visible = choosingPrompts,
            enter = expandVertically(boomSpatialSpec()) + fadeIn(boomEffectsSpec()),
            exit = shrinkVertically(boomSpatialSpec()) + fadeOut(boomEffectsSpec()),
        ) {
            val now = System.currentTimeMillis()
            val remaining = prompts.snoozedUntilMs - now
            val snoozed = remaining > 0
            val normal = !snoozed && prompts.skippedVersion == null
            // A pausa não guarda o período escolhido: mais de uma semana restando = "um mês".
            val week = snoozed && remaining <= WEEK_MS
            val month = snoozed && remaining > WEEK_MS
            Column {
                PromptChoice(R.string.feature_settings_update_prompts_always, normal) { updatesViewModel.resumePrompts() }
                PromptChoice(R.string.feature_settings_update_prompt_week, week) { updatesViewModel.snooze(SnoozePeriod.Week) }
                PromptChoice(R.string.feature_settings_update_prompt_month, month) { updatesViewModel.snooze(SnoozePeriod.Month) }
            }
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

private const val WEEK_MS = 7L * 24 * 60 * 60 * 1000

@Composable
private fun PromptChoice(@androidx.annotation.StringRes title: Int, selected: Boolean, onClick: () -> Unit) {
    BoomSettingsItem(
        icon = null,
        title = stringResource(title),
        onClick = onClick,
        trailing = { RadioButton(selected = selected, onClick = onClick) },
    )
}
