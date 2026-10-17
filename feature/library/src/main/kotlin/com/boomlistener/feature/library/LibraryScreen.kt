// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.boomlistener.core.designsystem.component.BoomPlaceholder

@Composable
internal fun LibraryScreen(
    onOpenArtist: (artistId: String) -> Unit,
    onOpenTagEditor: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
