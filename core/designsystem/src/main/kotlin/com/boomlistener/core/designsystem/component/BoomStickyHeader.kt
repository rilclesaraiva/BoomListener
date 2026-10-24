// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.relocation.BringIntoViewModifierNode

/*
 * Cabeçalho preso no topo de uma lista (a busca do Início e a de Artistas).
 *
 * Preso, ele fica embaixo da faixa das notificações, que precisa ficar sólida
 * (senão a lista aparece por trás dos ícones do sistema). Por isso ele começa
 * com uma faixa do tamanho dela. Fora do topo, essa faixa é transparente e
 * fica por cima do fim do item anterior ([tuckUnderStickyHeader]), então a
 * busca aparece logo abaixo do conteúdo, sem um vão grande no meio.
 *
 * Os pedidos de "mostrar na tela" que nascem dentro dele (o campo de texto
 * pede a cada letra para o cursor aparecer) param aqui ([KeepListStill]). A
 * lista desconta o cabeçalho preso da área visível, acha que o cursor está
 * escondido acima dele e rola para trás: a busca descolava do topo a cada
 * letra, a faixa voltava a ser transparente e apareciam restos do que vem
 * antes (o tremor e a piscada). Preso no topo ele já está sempre à vista.
 */

/** Cabeçalho preso com a faixa das notificações embutida (sólida só quando [stuck]). */
@Composable
fun BoomStickyHeader(
    stuck: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val color = MaterialTheme.colorScheme.surface
    Column(modifier.fillMaxWidth().then(KeepListStill)) {
        Spacer(
            Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(if (stuck) color else Color.Transparent),
        )
        Column(Modifier.fillMaxWidth().background(color)) { content() }
    }
}

/**
 * No item logo antes do [BoomStickyHeader]: diz à lista que ele é mais baixo
 * (a altura da faixa das notificações), para a faixa transparente do
 * cabeçalho ficar por cima do fim dele em vez de abrir um vão.
 */
@Composable
fun Modifier.tuckUnderStickyHeader(): Modifier {
    val overlap = WindowInsets.statusBars.getTop(LocalDensity.current)
    return layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, (placeable.height - overlap).coerceAtLeast(0)) { placeable.place(0, 0) }
    }
}

/**
 * Se o cabeçalho (item [headerIndex]) está preso no topo: só aí a faixa das
 * notificações acima dele fica sólida. Chegando perto ela continua
 * transparente (escurecer antes cortava o que passava por baixo); digitando
 * perto do topo, a tela leva a busca até lá e ela prende.
 */
@Composable
fun rememberStickyHeaderStuck(state: LazyListState, headerIndex: Int): State<Boolean> =
    remember(state, headerIndex) { derivedStateOf { state.firstVisibleItemIndex >= headerIndex } }

/** Segura os pedidos de "mostrar na tela" de dentro do cabeçalho (não rola a lista). */
private data object KeepListStill : ModifierNodeElement<KeepListStillNode>() {
    override fun create() = KeepListStillNode()
    override fun update(node: KeepListStillNode) = Unit
}

private class KeepListStillNode : Modifier.Node(), BringIntoViewModifierNode {
    // Não repassa ao pai de propósito: quem leva a busca ao topo é a tela.
    override suspend fun bringIntoView(childCoordinates: LayoutCoordinates, boundsProvider: () -> Rect?) = Unit
}
