// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import com.boomlistener.core.designsystem.component.BoomModalSheet
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.player.AudioOutput
import com.boomlistener.core.player.AudioOutputKind
import com.boomlistener.core.player.AudioOutputsData

/**
 * Botão do topo do player com o ícone de onde o som está saindo (celular,
 * fone, Bluetooth…). Tocar abre a lista de dispositivos conectados.
 */
@Composable
internal fun DevicesPill(modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val outputs = remember { AudioOutputsData.get(context) }
    val list by outputs.outputs.collectAsStateWithLifecycle()
    var open by remember { mutableStateOf(false) }
    val active = list.firstOrNull { it.active }

    // Pílula com o nome da saída que está tocando ("Fone Bluetooth").
    PlayerPill(
        icon = active?.kind?.icon() ?: BoomIcons.Devices,
        text = active?.displayName() ?: stringResource(R.string.feature_nowplaying_devices),
        onClick = { open = true },
        modifier = modifier,
    )
    if (open) DevicesSheet(list = list, onDismiss = { open = false })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DevicesSheet(list: List<AudioOutput>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val outputs = remember { AudioOutputsData.get(context.applicationContext) }
    BoomModalSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        ) {
            Text(
                stringResource(R.string.feature_nowplaying_devices),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Text(
                stringResource(R.string.feature_nowplaying_devices_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Spacer(Modifier.height(4.dp))
            list.forEach { output ->
                DeviceRow(output, onClick = { outputs.choose(output.id) })
            }
            Spacer(Modifier.height(4.dp))
            FilledTonalButton(
                onClick = { outputs.openSystemPicker(context) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(BoomIcons.Devices, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.feature_nowplaying_devices_more))
            }
        }
    }
}

@Composable
private fun DeviceRow(output: AudioOutput, onClick: () -> Unit) {
    val container = if (output.active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val content = if (output.active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Surface(
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge).clickable(onClick = onClick),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (output.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
            ) {
                Icon(
                    output.kind.icon(),
                    contentDescription = null,
                    tint = if (output.active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = output.displayName(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        if (output.active) R.string.feature_nowplaying_device_playing else R.string.feature_nowplaying_device_connected,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun AudioOutput.displayName(): String = when (kind) {
    AudioOutputKind.Phone -> stringResource(R.string.feature_nowplaying_device_phone)
    AudioOutputKind.Wired -> name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.feature_nowplaying_device_wired)
    AudioOutputKind.Bluetooth -> name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.feature_nowplaying_device_bluetooth)
    AudioOutputKind.Usb -> name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.feature_nowplaying_device_usb)
    AudioOutputKind.Hdmi -> name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.feature_nowplaying_device_hdmi)
}

private fun AudioOutputKind.icon(): ImageVector = when (this) {
    AudioOutputKind.Phone -> BoomIcons.Smartphone
    AudioOutputKind.Wired, AudioOutputKind.Usb -> BoomIcons.Headphones
    AudioOutputKind.Bluetooth -> BoomIcons.Bluetooth
    AudioOutputKind.Hdmi -> BoomIcons.Tv
}
