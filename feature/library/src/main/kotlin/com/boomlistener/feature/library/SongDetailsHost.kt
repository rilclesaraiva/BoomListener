// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.boomlistener.core.designsystem.component.BoomSongDetails
import com.boomlistener.core.designsystem.component.BoomSongDetailsDialog
import com.boomlistener.data.library.LibraryData

/** "Detalhes" de uma música (arquivo, formato, taxa de bits). Devolve a função que abre a janela. */
@Composable
internal fun rememberSongDetails(): (uri: String, title: String) -> Unit {
    val context = LocalContext.current
    val library = remember { LibraryData.repository(context.applicationContext) }
    var open by remember { mutableStateOf<Pair<String, String>?>(null) }
    var details by remember { mutableStateOf<BoomSongDetails?>(null) }
    open?.let { (uri, title) ->
        LaunchedEffect(uri) {
            details = null
            val found = library.details(uri)
            if (found == null) {
                open = null
                return@LaunchedEffect
            }
            details = BoomSongDetails(
                path = found.path,
                fileName = found.fileName,
                sizeBytes = found.sizeBytes,
                modifiedMs = found.modifiedMs,
                format = found.format,
                durationMs = found.durationMs,
                bitrateKbps = found.bitrateKbps,
                sampleRateHz = found.sampleRateHz,
            )
        }
        BoomSongDetailsDialog(title = title, details = details, onDismiss = { open = null })
    }
    return remember { { uri, title -> open = uri to title } }
}
