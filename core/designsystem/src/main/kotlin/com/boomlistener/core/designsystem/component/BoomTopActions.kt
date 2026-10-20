// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.boomlistener.core.designsystem.R

/**
 * Os botões do canto superior direito das abas principais, iguais em todas:
 * lançamentos dos artistas que você segue (o sino) e ajustes. O histórico
 * fica dentro do seu perfil (aba Perfil).
 */
@Composable
fun BoomTopActions(onOpenReleases: () -> Unit, onOpenSettings: () -> Unit) {
    IconButton(onClick = onOpenReleases) {
        Icon(BoomIcons.Releases, contentDescription = stringResource(R.string.core_designsystem_releases))
    }
    IconButton(onClick = onOpenSettings) {
        Icon(BoomIcons.Settings, contentDescription = stringResource(R.string.core_designsystem_settings))
    }
}
