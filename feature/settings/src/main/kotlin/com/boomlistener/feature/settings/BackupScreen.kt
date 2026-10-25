// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.net.Uri
import com.boomlistener.core.designsystem.component.BoomAccent
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.designsystem.component.BoomPageTitle
import com.boomlistener.core.designsystem.component.BoomScreenScaffold
import com.boomlistener.core.designsystem.component.BoomSettingsItem
import com.boomlistener.core.designsystem.component.BoomSettingsSection
import java.time.LocalDate
import kotlinx.coroutines.launch

/** Ajustes → Backup: salvar os dados do app num arquivo e trazê-los de volta. */
@Composable
internal fun BackupScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val version = appVersionName(context)
    var working by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Int?>(null) }
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        working = true
        scope.launch {
            message = runCatching { exportBackup(context.applicationContext, uri, version) }
                .fold({ R.string.feature_settings_backup_saved }, { R.string.feature_settings_backup_failed })
            working = false
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingRestore = uri
    }

    pendingRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text(stringResource(R.string.feature_settings_backup_confirm_title)) },
            text = { Text(stringResource(R.string.feature_settings_backup_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestore = null
                    working = true
                    scope.launch {
                        val result = runCatching { restoreBackup(context.applicationContext, uri) }.getOrNull()
                        working = false
                        when (result) {
                            RestoreResult.Done -> restartApp(context.applicationContext)
                            RestoreResult.Newer -> message = R.string.feature_settings_backup_newer
                            RestoreResult.NotABackup -> message = R.string.feature_settings_backup_invalid
                            null -> message = R.string.feature_settings_backup_restore_failed
                        }
                    }
                }) { Text(stringResource(R.string.feature_settings_backup_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestore = null }) { Text(stringResource(R.string.feature_settings_backup_cancel)) }
            },
        )
    }

    BoomScreenScaffold(
        title = stringResource(R.string.feature_settings_backup),
        onBack = onBack,
        modifier = modifier,
        topBar = {},
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            item(key = "pageTitle") {
                BoomPageTitle(stringResource(R.string.feature_settings_backup), hasBack = true, large = true)
            }
            item(key = "actions") {
                BoomSettingsSection(title = null) {
                    BoomSettingsItem(
                        icon = BoomIcons.Backup,
                        accent = BoomAccent.Primary,
                        title = stringResource(R.string.feature_settings_backup_export),
                        subtitle = stringResource(R.string.feature_settings_backup_export_summary),
                        enabled = !working,
                        onClick = { exportLauncher.launch("boomlistener-backup-${LocalDate.now()}.zip") },
                    )
                    BoomSettingsItem(
                        icon = BoomIcons.Restore,
                        accent = BoomAccent.Secondary,
                        title = stringResource(R.string.feature_settings_backup_restore),
                        subtitle = stringResource(R.string.feature_settings_backup_restore_summary),
                        enabled = !working,
                        onClick = { restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                    )
                }
            }
            item(key = "status") {
                val text = when {
                    working -> stringResource(R.string.feature_settings_backup_working)
                    else -> message?.let { stringResource(it) }
                }
                Text(
                    text ?: stringResource(R.string.feature_settings_backup_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
