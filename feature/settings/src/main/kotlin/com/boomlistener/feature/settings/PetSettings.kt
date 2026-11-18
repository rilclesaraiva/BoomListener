// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.core.designsystem.component.BoomSettingsItem
import com.boomlistener.core.designsystem.component.BoomSettingsSection
import com.boomlistener.data.profile.PetBackgroundKind
import com.boomlistener.data.profile.PetData
import kotlinx.coroutines.launch

/** Arquivos que o seletor oferece: o .zip de um shimeji ou uma imagem. */
private val PET_MIME_TYPES = arrayOf(
    "application/zip",
    "application/x-zip-compressed",
    "application/octet-stream",
    "image/png",
    "image/jpeg",
    "image/webp",
)

/** Ajustes → Aparência → bichinho do perfil: liga e desliga, troca ou remove o bichinho e escolhe o fundo da caixa. */
@Composable
internal fun PetSettingsSection() {
    val context = LocalContext.current.applicationContext
    val repository = remember { PetData.repository(context) }
    val pet by repository.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var failed by remember { mutableStateOf(false) }
    var colorDialog by remember { mutableStateOf(false) }

    val packPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch { failed = !repository.importPack(uri.toString()) }
    }
    // Imagens de fundo pelo seletor de fotos do sistema (sem pedir acesso às fotos).
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { repository.setBackgroundImage(uri.toString()) }
    }

    BoomSettingsSection(title = stringResource(R.string.feature_settings_pet)) {
        BoomSettingsItem(
            icon = null,
            title = stringResource(R.string.feature_settings_pet_show),
            subtitle = stringResource(R.string.feature_settings_pet_show_summary),
            onClick = { scope.launch { repository.setEnabled(!pet.enabled) } },
            trailing = { Switch(checked = pet.enabled, onCheckedChange = { scope.launch { repository.setEnabled(it) } }) },
        )
        BoomSettingsItem(
            icon = null,
            title = stringResource(if (pet.pack == null) R.string.feature_settings_pet_choose else R.string.feature_settings_pet_change),
            subtitle = if (failed) {
                stringResource(R.string.feature_settings_pet_failed)
            } else {
                pet.pack?.name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.feature_settings_pet_choose_summary)
            },
            onClick = {
                failed = false
                packPicker.launch(PET_MIME_TYPES)
            },
        )
        if (pet.pack != null) {
            BoomSettingsItem(
                icon = null,
                title = stringResource(R.string.feature_settings_pet_remove),
                onClick = { scope.launch { repository.removePack() } },
            )
        }
    }
    BoomSettingsSection(title = stringResource(R.string.feature_settings_pet_background)) {
        BackgroundOption(
            selected = pet.backgroundKind == PetBackgroundKind.Profile,
            title = stringResource(R.string.feature_settings_pet_bg_default),
            summary = stringResource(R.string.feature_settings_pet_bg_default_summary),
        ) { scope.launch { repository.setBackground(PetBackgroundKind.Profile) } }
        BackgroundOption(
            selected = pet.backgroundKind == PetBackgroundKind.Color,
            title = stringResource(R.string.feature_settings_pet_bg_color),
            summary = pet.backgroundColor.ifEmpty { stringResource(R.string.feature_settings_pet_bg_color_summary) },
        ) { colorDialog = true }
        BackgroundOption(
            selected = pet.backgroundKind == PetBackgroundKind.Image,
            title = stringResource(R.string.feature_settings_pet_bg_image),
            summary = stringResource(R.string.feature_settings_pet_bg_image_summary),
        ) { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        BackgroundOption(
            selected = pet.backgroundKind == PetBackgroundKind.None,
            title = stringResource(R.string.feature_settings_pet_bg_none),
            summary = stringResource(R.string.feature_settings_pet_bg_none_summary),
        ) { scope.launch { repository.setBackground(PetBackgroundKind.None) } }
    }

    if (colorDialog) {
        var text by remember { mutableStateOf(pet.backgroundColor) }
        var invalid by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { colorDialog = false },
            title = { Text(stringResource(R.string.feature_settings_pet_color_title)) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        invalid = false
                    },
                    singleLine = true,
                    isError = invalid,
                    placeholder = { Text(stringResource(R.string.feature_settings_pet_color_hint)) },
                    supportingText = if (invalid) {
                        { Text(stringResource(R.string.feature_settings_pet_color_invalid)) }
                    } else {
                        null
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        if (repository.setBackground(PetBackgroundKind.Color, text)) colorDialog = false else invalid = true
                    }
                }) { Text(stringResource(R.string.feature_settings_pet_color_save)) }
            },
            dismissButton = {
                TextButton(onClick = { colorDialog = false }) { Text(stringResource(R.string.feature_settings_backup_cancel)) }
            },
        )
    }
}

@Composable
private fun BackgroundOption(selected: Boolean, title: String, summary: String, onClick: () -> Unit) {
    BoomSettingsItem(
        icon = null,
        title = title,
        subtitle = summary,
        onClick = onClick,
        trailing = { RadioButton(selected = selected, onClick = onClick) },
    )
}
