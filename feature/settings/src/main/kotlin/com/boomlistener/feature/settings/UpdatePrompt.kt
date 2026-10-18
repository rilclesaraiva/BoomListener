// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.data.updates.UpdateCheckResult
import com.boomlistener.data.updates.UpdatesData

/**
 * Aviso de versão nova que aparece sozinho ao abrir o app (colocado pelo
 * :app por cima de tudo). Respeita o "adiar" e o "pular esta versão".
 */
@Composable
fun UpdatePromptHost() {
    val context = LocalContext.current
    val viewModel: UpdatePromptViewModel = viewModel {
        UpdatePromptViewModel(UpdatesData.repository(context.applicationContext))
    }
    val available by viewModel.available.collectAsStateWithLifecycle()
    available?.let { update ->
        UpdatePromptDialog(
            update = update,
            onDownload = {
                viewModel.onDownload()
                context.openUrl(update.downloadUrl ?: update.pageUrl)
            },
            onLater = viewModel::later,
            onSnooze = viewModel::snooze,
            onSkip = viewModel::skipVersion,
        )
    }
}

/** A tela inteira do aviso: versão, o que mudou e os botões. */
@Composable
internal fun UpdatePromptDialog(
    update: UpdateCheckResult.Available,
    onDownload: () -> Unit,
    onLater: () -> Unit,
    onSnooze: (SnoozePeriod) -> Unit,
    onSkip: () -> Unit,
) {
    Dialog(
        onDismissRequest = onLater,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                ) {
                    Spacer(Modifier.height(40.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(72.dp),
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(BoomIcons.NewReleases, contentDescription = null, modifier = Modifier.size(40.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        stringResource(R.string.feature_settings_update_prompt_title),
                        style = MaterialTheme.typography.displaySmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.feature_settings_update_prompt_versions, update.latestVersion, update.currentVersion),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(24.dp))
                    ReleaseNotes(update.notes)
                    Spacer(Modifier.height(24.dp))
                }
                HorizontalDivider()
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                ) {
                    Button(onClick = onDownload, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Text(stringResource(R.string.feature_settings_updates_download))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = onLater, modifier = Modifier.weight(1f).height(52.dp)) {
                            Text(stringResource(R.string.feature_settings_update_prompt_later))
                        }
                        SnoozeButton(onSnooze = onSnooze, onSkip = onSkip)
                    }
                }
            }
        }
    }
}

/** Sino cortado: silencia os avisos por um tempo, ou pula esta versão. */
@Composable
private fun SnoozeButton(onSnooze: (SnoozePeriod) -> Unit, onSkip: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedIconButton(onClick = { open = true }, modifier = Modifier.size(width = 72.dp, height = 52.dp)) {
            Icon(BoomIcons.NotificationsOff, contentDescription = stringResource(R.string.feature_settings_update_prompt_snooze))
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.feature_settings_update_prompt_week)) },
                onClick = {
                    open = false
                    onSnooze(SnoozePeriod.Week)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.feature_settings_update_prompt_month)) },
                onClick = {
                    open = false
                    onSnooze(SnoozePeriod.Month)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.feature_settings_update_prompt_skip)) },
                onClick = {
                    open = false
                    onSkip()
                },
            )
        }
    }
}

/** As notas da Release, com títulos, itens e negrito. */
@Composable
private fun ReleaseNotes(markdown: String) {
    val blocks = remember(markdown) { parseReleaseNotes(markdown) }
    if (blocks.isEmpty()) {
        Text(
            stringResource(R.string.feature_settings_update_prompt_no_notes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block ->
            when (block) {
                is NoteBlock.Heading -> Text(
                    rich(block.text),
                    style = if (block.level <= 2) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 12.dp),
                )
                is NoteBlock.Bullet -> Row(Modifier.padding(start = (block.depth * 16).dp)) {
                    Text("•", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text(rich(block.text), style = MaterialTheme.typography.bodyLarge)
                }
                is NoteBlock.Paragraph -> Text(rich(block.text), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

private fun rich(text: String) = buildAnnotatedString {
    boldRuns(text).forEach { (part, bold) ->
        if (bold) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(part) } else append(part)
    }
}
