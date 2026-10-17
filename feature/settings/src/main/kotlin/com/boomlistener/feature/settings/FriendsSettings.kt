// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.designsystem.component.BoomScreenScaffold
import com.boomlistener.core.designsystem.component.BoomSettingsItem
import com.boomlistener.core.designsystem.component.BoomSettingsSection
import com.boomlistener.data.social.SocialAccount
import com.boomlistener.data.social.SocialRepository
import com.boomlistener.data.social.SocialResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Ajustes de Amigos: compartilhar o que ouve e apagar o perfil. */
internal class FriendsSettingsViewModel(private val social: SocialRepository) : ViewModel() {
    val account: StateFlow<SocialAccount?> = social.account

    private val _failed = MutableStateFlow(false)
    /** A última mudança não chegou ao servidor (sem internet). */
    val failed: StateFlow<Boolean> = _failed.asStateFlow()

    fun setSharing(enabled: Boolean) {
        viewModelScope.launch {
            _failed.value = social.updateProfile(sharing = enabled) !is SocialResult.Ok
            // Desligou: some também o "ouvindo agora" que estava lá.
            if (!enabled) social.publishNowPlaying(null)
        }
    }

    fun deleteProfile() {
        viewModelScope.launch { _failed.value = social.deleteAccount() !is SocialResult.Ok }
    }
}

@Composable
internal fun FriendsSettingsScreen(
    viewModel: FriendsSettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val account by viewModel.account.collectAsStateWithLifecycle()
    val failed by viewModel.failed.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    BoomScreenScaffold(title = stringResource(R.string.feature_settings_friends), onBack = onBack, modifier = modifier) { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                val me = account
                if (me == null) {
                    Text(
                        stringResource(R.string.feature_settings_friends_no_profile),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp),
                    )
                } else {
                    BoomSettingsSection(title = null) {
                        BoomSettingsItem(
                            icon = BoomIcons.Person,
                            title = me.name,
                            subtitle = "@" + me.handle,
                        )
                        BoomSettingsItem(
                            icon = BoomIcons.MusicNote,
                            title = stringResource(R.string.feature_settings_friends_share),
                            subtitle = stringResource(R.string.feature_settings_friends_share_summary),
                            onClick = { viewModel.setSharing(!me.sharing) },
                            trailing = { Switch(checked = me.sharing, onCheckedChange = viewModel::setSharing) },
                        )
                        BoomSettingsItem(
                            icon = BoomIcons.Remove,
                            title = stringResource(R.string.feature_settings_friends_delete),
                            subtitle = stringResource(R.string.feature_settings_friends_delete_summary),
                            onClick = { confirmDelete = true },
                        )
                    }
                    if (failed) {
                        Text(
                            stringResource(R.string.feature_settings_friends_offline),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.feature_settings_friends_delete_title)) },
            text = { Text(stringResource(R.string.feature_settings_friends_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteProfile()
                }) { Text(stringResource(R.string.feature_settings_friends_delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.feature_settings_friends_cancel)) }
            },
        )
    }
}
