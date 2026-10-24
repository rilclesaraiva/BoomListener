// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity

/*
 * Espaço no fim da lista enquanto há busca.
 *
 * Com poucos resultados, a lista ficaria mais curta que a tela e desceria
 * sozinha (a busca pularia de lugar a cada letra). Então, logo que os
 * resultados mudam, o espaço tem a altura da tela inteira; assim que a lista
 * se mede, ele encolhe para o tamanho exato que deixa a busca no topo com o
 * último resultado no fim da tela. Encolher para o exato nunca faz a lista
 * descer; já começar pequeno faria (por isso não se calcula pela medida
 * anterior, que é a dos resultados antigos). Antes, ficava sempre a tela
 * inteira, e no fim dos resultados sobrava um vazio enorme.
 */

/** Guarda a altura do espaço; [resultsKey] muda junto com os resultados. */
internal class SearchRoom {
    var px by mutableIntStateOf(FULL)
}

@Composable
internal fun rememberSearchRoom(state: LazyListState, headerIndex: Int, resultsKey: Any?): SearchRoom {
    val room = remember(resultsKey) { SearchRoom() }
    LaunchedEffect(room, state, headerIndex) {
        // Espera um quadro: só então a lista já se mediu com os resultados novos
        // (a medida anterior ainda é a dos antigos e daria um espaço errado).
        withFrameNanos { }
        snapshotFlow { state.exactSearchRoom(headerIndex) }.collect { exact -> if (exact != null) room.px = exact }
    }
    return room
}

internal fun LazyListScope.searchRoomItem(room: SearchRoom) {
    item(key = SEARCH_ROOM_KEY) {
        if (room.px == FULL) {
            Spacer(Modifier.fillParentMaxHeight())
        } else {
            Spacer(Modifier.fillMaxWidth().height(with(LocalDensity.current) { room.px.toDp() }))
        }
    }
}

/**
 * Altura que deixa busca + resultados do tamanho exato da área da lista, ou
 * nulo quando a última medida não basta para saber (fica como está).
 */
private fun LazyListState.exactSearchRoom(headerIndex: Int): Int? {
    val info = layoutInfo
    val items = info.visibleItemsInfo
    val room = items.firstOrNull { it.key == SEARCH_ROOM_KEY }
    if (room == null) {
        // Busca no topo e o espaço fora da tela: os resultados já enchem a tela.
        return if (items.isNotEmpty() && firstVisibleItemIndex >= headerIndex) 0 else null
    }
    val header = items.firstOrNull { it.index == headerIndex } ?: return null
    val between = items.filter { it.index in (headerIndex + 1) until room.index }
    if (between.size != room.index - headerIndex - 1) return null
    val area = info.viewportSize.height - info.beforeContentPadding - info.afterContentPadding
    return (area - header.size - between.sumOf { it.size }).coerceAtLeast(0)
}

private const val SEARCH_ROOM_KEY = "searchRoom"
private const val FULL = -1
