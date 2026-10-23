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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Título que faz parte da página (a primeira linha da lista), no lugar de uma
 * barra presa na tela: fica no topo e sobe junto ao rolar. A tela usa
 * `BoomScreenScaffold(topBar = {}, onBack = …)` e põe este título como
 * primeiro item; o voltar fica parado no canto ([BoomFloatingBackButton]).
 *
 * @param hasBack a tela tem voltar: o título começa mais para o lado, para o
 *   botão parado nunca ficar por cima dele (nem ao rolar).
 * @param large título grande embaixo da linha do voltar (Ajustes).
 * @param statusBarInset desce o título para baixo da faixa das notificações.
 *   false quando a própria lista já começa abaixo dela (Início, que tem
 *   cabeçalho preso e não pode deixá-lo por baixo dessa faixa).
 */
@Composable
fun BoomPageTitle(
    title: String,
    modifier: Modifier = Modifier,
    hasBack: Boolean = false,
    large: Boolean = false,
    statusBarInset: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val start = if (hasBack) BoomBackButtonInset else 16.dp
    val insets = if (statusBarInset) modifier.windowInsetsPadding(WindowInsets.statusBars) else modifier
    Column(insets.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(start = start, end = 4.dp),
        ) {
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
                modifier = Modifier.padding(start = start, end = 16.dp, top = 8.dp, bottom = 20.dp),
            )
        }
    }
}
