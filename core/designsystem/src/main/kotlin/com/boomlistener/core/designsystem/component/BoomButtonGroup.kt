// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.boomlistener.core.designsystem.theme.boomEffectsSpec
import com.boomlistener.core.designsystem.theme.boomFastSpatialSpec
import com.boomlistener.core.designsystem.theme.boomSpatialSpec

/**
 * Grupo de botões do Material 3 Expressive: os botões dividem a largura toda,
 * lado a lado, e reagem juntos. Ao apertar, o botão "amassa" (cantos mais
 * retos) e se alarga um pouco, empurrando os vizinhos.
 *
 * Feito aqui (e não com o `ButtonGroup` do M3, que ainda é experimental e
 * muda de API entre versões) com peças estáveis: Row + Surface + molas do
 * motionScheme.
 */
@Composable
fun BoomButtonGroup(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(BoomButtonGroupSpacing),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
        content = content,
    )
}

/** Peso visual de um botão do grupo. */
enum class BoomGroupButtonStyle {
    /** A ação principal (cor primária). */
    Filled,

    /** Ações de apoio (cor secundária, mais suave). */
    Tonal,

    /** Liga/desliga: suave desligado, cor primária ligado. */
    Toggle,
}

/**
 * Um botão do [BoomButtonGroup].
 *
 * @param weight quanto da largura ele ocupa em relação aos outros.
 * @param round forma redonda (pílula); falso = mais quadrado. A troca é
 *   animada (ex.: o play é redondo pausado e quadrado tocando).
 * @param checked só para [BoomGroupButtonStyle.Toggle].
 */
@Composable
fun RowScope.BoomGroupButton(
    onClick: () -> Unit,
    height: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    weight: Float = 1f,
    style: BoomGroupButtonStyle = BoomGroupButtonStyle.Tonal,
    round: Boolean = false,
    checked: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    // Apertado: alarga um pouco (os vizinhos encolhem) e os cantos ficam mais retos.
    val animatedWeight by animateFloatAsState(
        targetValue = if (pressed) weight * PressedWidthRatio else weight,
        animationSpec = boomFastSpatialSpec(),
        label = "groupButtonWidth",
    )
    val cornerTarget = when {
        pressed -> height * PressedCornerRatio
        round -> height / 2
        else -> height * SquareCornerRatio
    }
    val corner by animateDpAsState(cornerTarget, boomSpatialSpec(), label = "groupButtonCorner")

    val colors = MaterialTheme.colorScheme
    val (container, contentTarget) = when (style) {
        BoomGroupButtonStyle.Filled -> colors.primary to colors.onPrimary
        BoomGroupButtonStyle.Tonal -> colors.secondaryContainer to colors.onSecondaryContainer
        BoomGroupButtonStyle.Toggle ->
            if (checked) colors.primary to colors.onPrimary else colors.surfaceContainerHighest to colors.onSurfaceVariant
    }
    val containerColor by animateColorAsState(container, boomEffectsSpec(), label = "groupButtonColor")
    val contentColor by animateColorAsState(contentTarget, boomEffectsSpec(), label = "groupButtonContent")

    val shape = RoundedCornerShape(corner.coerceIn(0.dp, height / 2))
    val described = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }
    val sized = modifier.weight(animatedWeight.coerceAtLeast(MinWeight)).height(height).then(described)

    if (style == BoomGroupButtonStyle.Toggle) {
        Surface(
            checked = checked,
            onCheckedChange = { onClick() },
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            interactionSource = interaction,
            modifier = sized,
        ) { GroupButtonContent(content) }
    } else {
        Surface(
            onClick = onClick,
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            interactionSource = interaction,
            modifier = sized,
        ) { GroupButtonContent(content) }
    }
}

@Composable
private fun GroupButtonContent(content: @Composable RowScope.() -> Unit) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 12.dp),
        content = content,
    )
}

/** Espaço entre os botões de um grupo. */
val BoomButtonGroupSpacing = 8.dp

private const val PressedWidthRatio = 1.15f
private const val PressedCornerRatio = 0.22f
private const val SquareCornerRatio = 0.32f
private const val MinWeight = 0.1f
