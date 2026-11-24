// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.boomlistener.core.designsystem.component.BoomSongDetails
import com.boomlistener.core.designsystem.component.BoomSongDetailsContent
import com.boomlistener.data.library.LibraryData

/**
 * Página "Detalhes" de uma música (arquivo, formato, taxa de bits), aberta dentro do painel
 * do menu (`BoomMenuAction.page`). Carrega os dados ao abrir; sem dados, mostra só o título.
 */
@Composable
internal fun SongDetailsPage(uri: String, title: String) {
    val context = LocalContext.current
    val library = remember { LibraryData.repository(context.applicationContext) }
    val details by produceState<BoomSongDetails?>(initialValue = null, uri) {
        val found = library.details(uri)
        value = BoomSongDetails(
            path = found?.path,
            fileName = found?.fileName,
            sizeBytes = found?.sizeBytes,
            modifiedMs = found?.modifiedMs,
            format = found?.format,
            durationMs = found?.durationMs,
            bitrateKbps = found?.bitrateKbps,
            sampleRateHz = found?.sampleRateHz,
        )
    }
    BoomSongDetailsContent(title = title, details = details)
}
