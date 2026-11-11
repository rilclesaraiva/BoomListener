// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.boomlistener.core.common.OfflineMode
import com.boomlistener.core.common.openUrl
import com.boomlistener.data.lastfm.LastfmData
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.designsystem.component.BoomPageTitle
import com.boomlistener.core.designsystem.component.BoomScreenScaffold
import com.boomlistener.core.designsystem.component.BoomSettingsItem
import com.boomlistener.core.designsystem.component.BoomSettingsSection
import com.boomlistener.core.player.PlaybackSettingsData

/**
 * Ajustes → Sistema: como o app usa a internet (modo offline, Last.fm), as
 * atualizações do app, o que ele
 * faz em segundo plano (fechar de vez ao tirar dos apps recentes) e o backup
 * dos seus dados.
 */
@Composable
internal fun SystemSettingsScreen(onBack: () -> Unit, onOpenBackup: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val playback = remember { PlaybackSettingsData.get(context) }
    val prefs by playback.prefs.collectAsStateWithLifecycle()
    val offline by OfflineMode.enabled.collectAsStateWithLifecycle()
    val lastfm: LastfmAccessViewModel = viewModel {
        LastfmAccessViewModel(LastfmData.keyStore(context), LastfmData.repository(context))
    }
    val lastfmState by lastfm.state.collectAsStateWithLifecycle()
    // "Minha própria chave" já aparece marcada ao tocar, antes de a chave ser aceita e salva.
    var choosingOwnKey by remember { mutableStateOf(false) }
    val ownKeySelected = lastfmState.usingOwnKey || choosingOwnKey

    BoomScreenScaffold(
        title = stringResource(R.string.feature_settings_system),
        onBack = onBack,
        modifier = modifier,
        // Sem barra presa na tela: o título é o topo da página.
        topBar = {},
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            item(key = "pageTitle") {
                BoomPageTitle(stringResource(R.string.feature_settings_system), hasBack = true, large = true)
            }
            item(key = "internet") {
                BoomSettingsSection(title = stringResource(R.string.feature_settings_system_internet)) {
                    // Modo 100% offline: nada vai para a internet (só a verificação de atualizações).
                    BoomSettingsItem(
                        icon = null,
                        title = stringResource(R.string.feature_settings_offline),
                        subtitle = stringResource(R.string.feature_settings_offline_summary),
                        onClick = { OfflineMode.set(context, !offline) },
                        trailing = { Switch(checked = offline, onCheckedChange = { OfflineMode.set(context, it) }) },
                    )
                }
            }
            item(key = "lastfm") {
                BoomSettingsSection(title = stringResource(R.string.feature_settings_lastfm)) {
                    // Servidor do Boom (padrão): a chave do projeto fica no servidor, não no app.
                    BoomSettingsItem(
                        icon = null,
                        title = stringResource(R.string.feature_settings_lastfm_boom),
                        subtitle = stringResource(R.string.feature_settings_lastfm_boom_summary),
                        onClick = { choosingOwnKey = false; lastfm.useBoomServer() },
                        trailing = {
                            RadioButton(selected = !ownKeySelected, onClick = { choosingOwnKey = false; lastfm.useBoomServer() })
                        },
                    )
                    // Chave própria: o aparelho fala direto com o Last.fm.
                    BoomSettingsItem(
                        icon = null,
                        title = stringResource(R.string.feature_settings_lastfm_own),
                        subtitle = lastfmState.savedKeyEnd?.let { stringResource(R.string.feature_settings_lastfm_using, it) }
                            ?: stringResource(R.string.feature_settings_lastfm_own_summary),
                        onClick = { choosingOwnKey = true },
                        trailing = { RadioButton(selected = ownKeySelected, onClick = { choosingOwnKey = true }) },
                    )
                    if (ownKeySelected) {
                        LastfmKeyForm(
                            state = lastfmState,
                            onInput = lastfm::onInput,
                            onSave = lastfm::save,
                            onGetKey = { context.openUrl("https://www.last.fm/api/account/create") },
                        )
                    }
                }
            }
            item(key = "updates") { UpdatesSettingsSection() }
            item(key = "background") {
                BoomSettingsSection(title = stringResource(R.string.feature_settings_system_background)) {
                    // Tirar dos recentes encerra a música e o app (desligado: continua tocando).
                    BoomSettingsItem(
                        icon = null,
                        title = stringResource(R.string.feature_settings_stop_on_swipe),
                        subtitle = stringResource(R.string.feature_settings_stop_on_swipe_summary),
                        onClick = { playback.setStopOnTaskRemoved(!prefs.stopOnTaskRemoved) },
                        trailing = { Switch(checked = prefs.stopOnTaskRemoved, onCheckedChange = playback::setStopOnTaskRemoved) },
                    )
                }
            }
            item(key = "data") {
                BoomSettingsSection(title = stringResource(R.string.feature_settings_system_data)) {
                    // Backup dos dados do app num arquivo (e restaurar, inclusive num celular novo).
                    BoomSettingsItem(
                        icon = BoomIcons.Backup,
                        title = stringResource(R.string.feature_settings_backup),
                        subtitle = stringResource(R.string.feature_settings_backup_summary),
                        onClick = onOpenBackup,
                    )
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

/** Campo da chave própria do Last.fm, com o botão que a testa antes de salvar. */
@Composable
private fun LastfmKeyForm(
    state: LastfmAccessUiState,
    onInput: (String) -> Unit,
    onSave: () -> Unit,
    onGetKey: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.input,
                onValueChange = onInput,
                singleLine = true,
                enabled = !state.checking,
                isError = state.message == LastfmAccessMessage.BadFormat || state.message == LastfmAccessMessage.Rejected,
                label = { Text(stringResource(R.string.feature_settings_lastfm_key_hint)) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Ascii,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.keyRejected) {
                Text(
                    stringResource(R.string.feature_settings_lastfm_key_lost),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            state.message?.let { message ->
                Text(
                    stringResource(
                        when (message) {
                            LastfmAccessMessage.Saved -> R.string.feature_settings_lastfm_saved
                            LastfmAccessMessage.BadFormat -> R.string.feature_settings_lastfm_bad_format
                            LastfmAccessMessage.Rejected -> R.string.feature_settings_lastfm_rejected
                            LastfmAccessMessage.CheckFailed -> R.string.feature_settings_lastfm_failed
                            LastfmAccessMessage.SaveFailed -> R.string.feature_settings_lastfm_save_failed
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (message == LastfmAccessMessage.Saved) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(enabled = state.input.isNotBlank() && !state.checking, onClick = onSave) {
                    Text(
                        stringResource(
                            if (state.checking) R.string.feature_settings_lastfm_checking else R.string.feature_settings_lastfm_save,
                        ),
                    )
                }
                TextButton(onClick = onGetKey) { Text(stringResource(R.string.feature_settings_lastfm_get_key)) }
            }
        }
    }
}
