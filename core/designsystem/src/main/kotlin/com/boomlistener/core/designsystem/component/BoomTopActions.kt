// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.stringResource
import com.boomlistener.core.designsystem.R

/**
 * Tela larga (tablet, celular deitado): o app usa a barra lateral no lugar
 * da barra de abas. O :app fornece.
 */
val LocalWideLayout = staticCompositionLocalOf { false }

/**
 * Botão do perfil (a sua foto), que na tela larga fica no topo à direita.
 * O :app fornece (o perfil mora em outro módulo); null = não mostra.
 */
val LocalProfileButton = staticCompositionLocalOf<(@Composable () -> Unit)?> { null }

/**
 * Os botões do canto superior direito das abas principais, iguais em todas:
 * lançamentos dos artistas que você segue (o sino) e ajustes. O histórico
 * fica dentro do seu perfil (aba Perfil). Na tela larga, o sino vai para a
 * barra lateral e aqui ficam Ajustes e a sua foto (o perfil).
 */
@Composable
fun BoomTopActions(onOpenReleases: () -> Unit, onOpenSettings: () -> Unit) {
    val wide = LocalWideLayout.current
    if (!wide) {
        IconButton(onClick = onOpenReleases) {
            Icon(BoomIcons.Releases, contentDescription = stringResource(R.string.core_designsystem_releases))
        }
    }
    IconButton(onClick = onOpenSettings) {
        Icon(BoomIcons.Settings, contentDescription = stringResource(R.string.core_designsystem_settings))
    }
    if (wide) LocalProfileButton.current?.invoke()
}
