// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.core.common.OfflineMode
import com.boomlistener.core.designsystem.component.BoomPageTitle
import com.boomlistener.core.designsystem.component.BoomScreenScaffold
import com.boomlistener.core.designsystem.component.BoomSettingsItem
import com.boomlistener.core.designsystem.component.BoomSettingsSection
import com.boomlistener.core.player.PlaybackSettingsData

/**
 * Ajustes → Sistema: como o app usa a internet (modo offline) e o que ele
 * faz em segundo plano (fechar de vez ao tirar dos apps recentes).
 */
@Composable
internal fun SystemSettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val playback = remember { PlaybackSettingsData.get(context) }
    val prefs by playback.prefs.collectAsStateWithLifecycle()
    val offline by OfflineMode.enabled.collectAsStateWithLifecycle()

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
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
