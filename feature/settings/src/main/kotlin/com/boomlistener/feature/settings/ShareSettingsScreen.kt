// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.core.common.ShareTarget
import com.boomlistener.core.designsystem.component.BoomPageTitle
import com.boomlistener.core.designsystem.component.BoomScreenScaffold
import com.boomlistener.core.designsystem.component.BoomSettingsItem
import com.boomlistener.core.designsystem.component.BoomSettingsSection

/**
 * Ajustes → Compartilhar: o app favorito, escolhido entre os que recebem
 * links ou pelo nome do pacote. Ele vira "Compartilhar com…" nos menus.
 */
@Composable
internal fun ShareSettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val favorite by ShareTarget.favorite(context).collectAsStateWithLifecycle()
    val apps = remember { ShareTarget.candidates(context) }
    var typed by remember { mutableStateOf("") }
    var notFound by remember { mutableStateOf(false) }

    BoomScreenScaffold(
        title = stringResource(R.string.feature_settings_share),
        onBack = onBack,
        modifier = modifier,
        // Sem barra presa na tela: o título é o topo da página.
        topBar = {},
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            item(key = "pageTitle") {
                BoomPageTitle(stringResource(R.string.feature_settings_share), onBack = onBack, large = true)
            }
            item(key = "hint") {
                Text(
                    stringResource(R.string.feature_settings_share_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            item(key = "package") {
                BoomSettingsSection(title = stringResource(R.string.feature_settings_share_by_package)) {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = typed,
                                    onValueChange = {
                                        typed = it.trim()
                                        notFound = false
                                    },
                                    singleLine = true,
                                    isError = notFound,
                                    placeholder = { Text("com.example.app") },
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(Modifier.width(8.dp))
                                FilledTonalButton(
                                    enabled = typed.isNotBlank(),
                                    onClick = { notFound = !ShareTarget.setFavorite(context, typed) },
                                ) { Text(stringResource(R.string.feature_settings_share_use)) }
                            }
                            if (notFound) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    stringResource(R.string.feature_settings_share_not_found),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
            item(key = "apps-title") {
                BoomSettingsSection(title = stringResource(R.string.feature_settings_share_apps)) {
                    BoomSettingsItem(
                        icon = null,
                        title = stringResource(R.string.feature_settings_share_none),
                        onClick = { ShareTarget.setFavorite(context, null) },
                        trailing = { RadioButton(selected = favorite == null, onClick = { ShareTarget.setFavorite(context, null) }) },
                    )
                }
            }
            items(apps, key = { it.packageName }) { app ->
                Column(Modifier.padding(horizontal = 16.dp, vertical = 1.dp)) {
                    BoomSettingsItem(
                        icon = null,
                        title = app.label,
                        subtitle = app.packageName,
                        onClick = { ShareTarget.setFavorite(context, app.packageName) },
                        trailing = {
                            RadioButton(
                                selected = favorite?.packageName == app.packageName,
                                onClick = { ShareTarget.setFavorite(context, app.packageName) },
                            )
                        },
                    )
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
