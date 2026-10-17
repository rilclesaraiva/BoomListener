// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.boomlistener.core.designsystem.R

/**
 * Os botões do canto superior direito das abas principais: histórico de
 * escuta e ajustes. Iguais em todas as abas.
 */
@Composable
fun BoomTopActions(onOpenHistory: () -> Unit, onOpenSettings: () -> Unit) {
    IconButton(onClick = onOpenHistory) {
        Icon(BoomIcons.History, contentDescription = stringResource(R.string.core_designsystem_history))
    }
    IconButton(onClick = onOpenSettings) {
        Icon(BoomIcons.Settings, contentDescription = stringResource(R.string.core_designsystem_settings))
    }
}
