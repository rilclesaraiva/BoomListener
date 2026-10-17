// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.boomlistener.core.common.MediaPermissions
import com.boomlistener.core.common.openAppSettings
import com.boomlistener.core.designsystem.component.BoomPlaceholder

@Composable
internal fun LibraryScreen(
    onOpenArtist: (artistId: String) -> Unit,
    onOpenTagEditor: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(MediaPermissions.hasReadAudio(context)) }
    // Depois de duas recusas o Android não mostra mais o pedido; aí só dá
    // para liberar pelas configurações do sistema.
    var denials by rememberSaveable { mutableIntStateOf(0) }

    // Confere de novo ao voltar das configurações do sistema.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasPermission = MediaPermissions.hasReadAudio(context)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
        if (!granted) denials++
    }

    if (!hasPermission) {
        AudioPermissionRequest(
            denied = denials > 0,
            showSettings = denials >= 2,
            onRequest = { permissionLauncher.launch(MediaPermissions.readAudio) },
            onOpenSettings = { context.openAppSettings() },
            modifier = modifier,
        )
    } else {
        BoomPlaceholder(
            title = stringResource(R.string.feature_library_title),
            message = stringResource(R.string.feature_library_message),
            modifier = modifier,
        ) {
            // Botões provisórios só para testar a navegação até as telas existirem.
            TextButton(onClick = { onOpenArtist("exemplo") }) { Text("Abrir artista de exemplo") }
            TextButton(onClick = onOpenTagEditor) { Text("Abrir editor de tags") }
            TextButton(onClick = onOpenNowPlaying) { Text("Abrir tocando agora") }
        }
    }
}

@Composable
private fun AudioPermissionRequest(
    denied: Boolean,
    showSettings: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoomPlaceholder(
        title = stringResource(R.string.feature_library_permission_title),
        message = stringResource(
            if (denied) R.string.feature_library_permission_denied
            else R.string.feature_library_permission_message,
        ),
        modifier = modifier,
    ) {
        if (showSettings) {
            Button(onClick = onOpenSettings) {
                Text(stringResource(R.string.feature_library_permission_settings))
            }
        } else {
            Button(onClick = onRequest) {
                Text(stringResource(R.string.feature_library_permission_button))
            }
        }
    }
}
