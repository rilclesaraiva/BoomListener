// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.boomlistener.core.designsystem.R

/** Uma opção do menu de três pontinhos. */
data class BoomMenuAction(
    val label: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

/**
 * Botão de três pontinhos com as opções de sempre para músicas e álbuns:
 * as [actions] próprias da tela e, por fim, compartilhar o link, ouvir no
 * YouTube Music e abrir no Deezer (cada uma só aparece se for passada).
 */
@Composable
fun BoomMoreMenu(
    actions: List<BoomMenuAction> = emptyList(),
    onShare: (() -> Unit)? = null,
    onYoutubeMusic: (() -> Unit)? = null,
    onDeezer: (() -> Unit)? = null,
) {
    var open by remember { mutableStateOf(false) }
    val all = buildList {
        addAll(actions)
        onShare?.let { add(BoomMenuAction(stringResource(R.string.core_designsystem_share), onClick = it)) }
        onYoutubeMusic?.let { add(BoomMenuAction(stringResource(R.string.core_designsystem_youtube_music), onClick = it)) }
        onDeezer?.let { add(BoomMenuAction(stringResource(R.string.core_designsystem_open_deezer), onClick = it)) }
    }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(BoomIcons.More, contentDescription = stringResource(R.string.core_designsystem_more))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            all.forEach { action ->
                DropdownMenuItem(
                    text = { Text(action.label) },
                    enabled = action.enabled,
                    onClick = {
                        open = false
                        action.onClick()
                    },
                )
            }
        }
    }
}
