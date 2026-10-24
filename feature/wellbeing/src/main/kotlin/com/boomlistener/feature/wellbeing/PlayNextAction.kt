// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.wellbeing

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.boomlistener.core.designsystem.component.BoomMenuAction
import com.boomlistener.core.designsystem.component.BoomMenuKind
import com.boomlistener.core.player.PlayerConnection
import androidx.compose.runtime.rememberCoroutineScope
import com.boomlistener.data.library.LibraryData
import kotlinx.coroutines.launch

/**
 * "Adicionar à fila" no menu de uma música: ela toca logo depois da atual.
 * Aqui só se tem o endereço da música; ela é achada na biblioteca.
 */
@Composable
internal fun playNextAction(uri: String): BoomMenuAction {
    val context = LocalContext.current.applicationContext
    val controller = remember { PlayerConnection.controller(context) }
    val library = remember { LibraryData.repository(context) }
    val scope = rememberCoroutineScope()
    val done = stringResource(R.string.feature_wellbeing_play_next_done)
    return BoomMenuAction(stringResource(R.string.feature_wellbeing_play_next), kind = BoomMenuKind.PlayNext) {
        scope.launch {
            val track = library.getTracks().firstOrNull { it.uri == uri } ?: return@launch
            controller.playNext(track)
            Toast.makeText(context, done, Toast.LENGTH_SHORT).show()
        }
    }
}
