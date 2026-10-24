// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.boomlistener.core.designsystem.component.BoomMenuAction
import com.boomlistener.core.designsystem.component.BoomMenuKind
import com.boomlistener.core.player.PlayerConnection
import com.boomlistener.core.model.Track

/**
 * "Adicionar à fila" no menu de uma música: ela toca logo depois da atual.
 */
@Composable
internal fun playNextAction(track: Track): BoomMenuAction {
    val context = LocalContext.current.applicationContext
    val controller = remember { PlayerConnection.controller(context) }
    val done = stringResource(R.string.feature_library_play_next_done)
    return BoomMenuAction(stringResource(R.string.feature_library_play_next), kind = BoomMenuKind.PlayNext) {
        controller.playNext(track)
        Toast.makeText(context, done, Toast.LENGTH_SHORT).show()
    }
}
