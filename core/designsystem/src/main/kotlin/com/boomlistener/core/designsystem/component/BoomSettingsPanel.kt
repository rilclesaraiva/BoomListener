// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.boomlistener.core.designsystem.theme.boomEffectsSpec
import com.boomlistener.core.designsystem.theme.boomSpatialSpec

/**
 * Uma opção dos Ajustes com lista: começa **sempre fechada**, mostrando só o título e o
 * que está escolhido ([summary]); ao tocar, abre as opções ([content]). Volta a
 * fechada ao sair da tela (o estado não é guardado de propósito).
 */
@Composable
fun BoomSettingsPanel(
    title: String,
    summary: String?,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (open) 180f else 0f, boomSpatialSpec(), label = "panelArrow")
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        BoomSettingsItem(
            icon = icon,
            title = title,
            subtitle = summary,
            onClick = { open = !open },
            trailing = { Icon(BoomIcons.Collapse, contentDescription = null, modifier = Modifier.rotate(rotation)) },
        )
        AnimatedVisibility(
            visible = open,
            enter = expandVertically(boomSpatialSpec<IntSize>()) + fadeIn(boomEffectsSpec()),
            exit = shrinkVertically(boomSpatialSpec<IntSize>()) + fadeOut(boomEffectsSpec()),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), content = content)
        }
    }
}
