// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import android.Manifest
import android.app.Activity
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.boomlistener.core.player.PlayerConnection
import com.boomlistener.data.library.DeleteResult
import com.boomlistener.data.library.LibraryData
import kotlinx.coroutines.launch

/**
 * "Apagar do aparelho": no Android 11+ o próprio sistema confirma; antes
 * disso o app pergunta. A lista se atualiza sozinha depois (o MediaStore avisa).
 * Devolve a função que começa a apagar uma música.
 */
@Composable
internal fun rememberSongDeleter(): (uri: String, title: String) -> Unit {
    val context = LocalContext.current
    val library = remember { LibraryData.repository(context.applicationContext) }
    // Apagada: sai também da fila de reprodução.
    val onDeleted: (String) -> Unit = { uri -> PlayerConnection.controller(context.applicationContext).removeUri(uri) }
    val scope = rememberCoroutineScope()
    var confirming by remember { mutableStateOf<Pair<String, String>?>(null) }
    var pending by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableStateOf<String?>(null) }
    val failedText = stringResource(R.string.feature_nowplaying_delete_failed)

    val systemConfirm = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val uri = pending
        if (result.resultCode == Activity.RESULT_OK && uri != null) {
            // Android 10: a pessoa liberou; agora apaga de verdade. Android 11+: já apagou.
            if (retry == uri) {
                retry = null
                scope.launch {
                    if (library.delete(listOf(uri)) == DeleteResult.Done) onDeleted(uri)
                    else Toast.makeText(context, failedText, Toast.LENGTH_SHORT).show()
                }
            } else {
                onDeleted(uri)
            }
        }
        pending = null
    }
    var askPermission by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val uri = pending
        if (granted && uri != null) {
            scope.launch {
                if (library.delete(listOf(uri)) == DeleteResult.Done) onDeleted(uri)
                else Toast.makeText(context, failedText, Toast.LENGTH_SHORT).show()
            }
        }
        pending = null
    }
    LaunchedEffect(askPermission) {
        if (askPermission) {
            askPermission = false
            permission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    fun start(uri: String) {
        scope.launch {
            when (val result = library.delete(listOf(uri))) {
                DeleteResult.Done -> onDeleted(uri)
                is DeleteResult.NeedsConfirmation -> {
                    pending = uri
                    retry = if (result.retryAfter) uri else null
                    systemConfirm.launch(IntentSenderRequest.Builder(result.intentSender).build())
                }
                DeleteResult.NeedsPermission -> {
                    pending = uri
                    askPermission = true
                }
                DeleteResult.Failed -> Toast.makeText(context, failedText, Toast.LENGTH_SHORT).show()
            }
        }
    }

    confirming?.let { (uri, title) ->
        AlertDialog(
            onDismissRequest = { confirming = null },
            title = { Text(stringResource(R.string.feature_nowplaying_delete_song_title)) },
            text = { Text(stringResource(R.string.feature_nowplaying_delete_song_message, title)) },
            confirmButton = {
                TextButton(onClick = {
                    confirming = null
                    start(uri)
                }) { Text(stringResource(R.string.feature_nowplaying_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirming = null }) { Text(stringResource(R.string.feature_nowplaying_cancel)) }
            },
        )
    }

    return { uri, title ->
        // Android 11+: o sistema já mostra o "Apagar?"; perguntar duas vezes cansa.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) start(uri) else confirming = uri to title
    }
}
