import com.boomlistener.core.common.DiagnosticLog
import com.boomlistener.core.designsystem.component.imeVisible
// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import com.boomlistener.core.designsystem.component.rememberKeyboardDismisser
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.boomlistener.core.model.Track
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
internal fun rememberSongDeleter(): (Track) -> Unit {
    val many = rememberSongsDeleter()
    return { track -> many(listOf(track)) }
}

/** O mesmo para várias músicas de uma vez (a seleção das listas). */
@Composable
internal fun rememberSongsDeleter(): (List<Track>) -> Unit {
    val context = LocalContext.current
    val library = remember { LibraryData.repository(context.applicationContext) }
    // Apagada: sai também da fila de reprodução.
    val onDeleted: (List<String>) -> Unit = { uris ->
        val player = PlayerConnection.controller(context.applicationContext)
        uris.forEach { player.removeUri(it) }
    }
    val scope = rememberCoroutineScope()
    var confirming by remember { mutableStateOf<List<Track>?>(null) }
    var pending by remember { mutableStateOf<List<String>?>(null) }
    var retry by remember { mutableStateOf(false) }
    val failedText = stringResource(R.string.feature_library_delete_failed)

    val systemConfirm = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        DiagnosticLog.log("apagar", "janela do sistema voltou, resultCode=${result.resultCode}")
        val uris = pending
        if (result.resultCode == Activity.RESULT_OK && uris != null) {
            // Android 10: a pessoa liberou; agora apaga de verdade. Android 11+: já apagou.
            if (retry) {
                retry = false
                scope.launch {
                    if (library.delete(uris) == DeleteResult.Done) onDeleted(uris)
                    else Toast.makeText(context, failedText, Toast.LENGTH_SHORT).show()
                }
            } else {
                onDeleted(uris)
            }
        }
        pending = null
    }
    var askPermission by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val uris = pending
        if (granted && uris != null) {
            scope.launch {
                if (library.delete(uris) == DeleteResult.Done) onDeleted(uris)
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

    val dismissKeyboard = rememberKeyboardDismisser()
    val view = LocalView.current
    fun start(uris: List<String>) {
        scope.launch {
            DiagnosticLog.log("apagar", "pedido (${uris.size}) na biblioteca, teclado=${imeVisible(view)}")
            dismissKeyboard()
            val result = library.delete(uris)
            DiagnosticLog.log("apagar", "resultado=${result::class.simpleName}, teclado=${imeVisible(view)}")
            when (result) {
                DeleteResult.Done -> onDeleted(uris)
                is DeleteResult.NeedsConfirmation -> {
                    pending = uris
                    retry = result.retryAfter
                    DiagnosticLog.log("apagar", "abrindo a janela do sistema, teclado=${imeVisible(view)}")
                    systemConfirm.launch(IntentSenderRequest.Builder(result.intentSender).build())
                }
                DeleteResult.NeedsPermission -> {
                    pending = uris
                    askPermission = true
                }
                DeleteResult.Failed -> Toast.makeText(context, failedText, Toast.LENGTH_SHORT).show()
            }
        }
    }

    confirming?.let { tracks ->
        AlertDialog(
            onDismissRequest = { confirming = null },
            title = {
                Text(
                    if (tracks.size == 1) stringResource(R.string.feature_library_delete_song_title)
                    else pluralStringResource(R.plurals.feature_library_delete_songs_title, tracks.size, tracks.size),
                )
            },
            text = {
                Text(
                    if (tracks.size == 1) stringResource(R.string.feature_library_delete_song_message, tracks.first().title)
                    else stringResource(R.string.feature_library_delete_songs_message),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirming = null
                    start(tracks.map { it.uri })
                }) { Text(stringResource(R.string.feature_library_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirming = null }) { Text(stringResource(R.string.feature_library_cancel)) }
            },
        )
    }

    return { tracks ->
        if (tracks.isNotEmpty()) {
            // Android 11+: o sistema já mostra o "Apagar?"; perguntar duas vezes cansa.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) start(tracks.map { it.uri }) else confirming = tracks
        }
    }
}
