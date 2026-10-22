// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.boomlistener.core.designsystem.R

/** Largura que o título deixa livre à esquerda para não ficar por baixo do [BoomBackButton]. */
val BoomBackButtonInset = 72.dp

/**
 * O "voltar" do app: a seta dentro de um círculo, sempre igual em toda tela
 * (artista, álbum, Ajustes, telas de dentro). Nas telas sem barra presa
 * ([BoomFloatingBackButton]) fica parado no canto, por cima da página.
 */
@Composable
fun BoomBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    ) {
        IconButton(onClick = onClick) {
            Icon(BoomIcons.Back, contentDescription = stringResource(R.string.core_designsystem_back))
        }
    }
}

/** O [BoomBackButton] parado no canto de cima, abaixo da faixa das notificações. */
@Composable
fun BoomFloatingBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    BoomBackButton(onClick, modifier.statusBarsPadding().padding(8.dp))
}
