// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import com.boomlistener.core.designsystem.component.rememberKeyboardDismisser
import android.Manifest
import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import com.boomlistener.core.designsystem.component.BoomConfirmPage
import com.boomlistener.core.designsystem.component.BoomMenuPageScope
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
    var pending by remember { mutableStateOf<List<String>?>(null) }
    var retry by remember { mutableStateOf(false) }
    val failedText = stringResource(R.string.feature_library_delete_failed)

    val systemConfirm = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
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
    fun start(uris: List<String>) {
        scope.launch {
            dismissKeyboard()
            val result = library.delete(uris)
            when (result) {
                DeleteResult.Done -> onDeleted(uris)
                is DeleteResult.NeedsConfirmation -> {
                    pending = uris
                    retry = result.retryAfter
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

    // A pergunta "Apagar?" é uma página do painel do menu ([DeleteSongsPage]): aqui só se apaga.
    return { tracks -> if (tracks.isNotEmpty()) start(tracks.map { it.uri }) }
}

/** Página de confirmação de apagar uma ou mais músicas, dentro do painel do menu. */
@Composable
internal fun BoomMenuPageScope.DeleteSongsPage(tracks: List<Track>, onDelete: () -> Unit) {
    BoomConfirmPage(
        title = if (tracks.size == 1) stringResource(R.string.feature_library_delete_song_title)
        else pluralStringResource(R.plurals.feature_library_delete_songs_title, tracks.size, tracks.size),
        message = if (tracks.size == 1) stringResource(R.string.feature_library_delete_song_message, tracks.first().title)
        else stringResource(R.string.feature_library_delete_songs_message),
        confirmLabel = stringResource(R.string.feature_library_delete),
        cancelLabel = stringResource(R.string.feature_library_cancel),
        onConfirm = onDelete,
    )
}
