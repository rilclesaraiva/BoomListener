// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.boomlistener.core.designsystem.R

/**
 * Título que faz parte da página (a primeira linha da lista), no lugar de uma
 * barra presa na tela: fica no topo e sobe junto ao rolar. A tela usa
 * `BoomScreenScaffold(topBar = {})` e põe este título como primeiro item.
 *
 * @param large título grande embaixo da seta de voltar (Ajustes).
 */
@Composable
fun BoomPageTitle(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    large: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(start = if (onBack != null) 4.dp else 16.dp, end = 4.dp),
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(BoomIcons.Back, contentDescription = stringResource(R.string.core_designsystem_back))
                }
                Spacer(Modifier.width(4.dp))
            }
            if (!large) {
                Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            } else {
                Spacer(Modifier.weight(1f))
            }
            actions()
        }
        if (large) {
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 20.dp),
            )
        }
    }
}
