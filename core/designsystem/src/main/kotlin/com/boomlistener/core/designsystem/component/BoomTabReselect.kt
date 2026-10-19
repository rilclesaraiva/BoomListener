// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter

/**
 * Avisos de "tocou de novo na aba que já está aberta", com a rota da aba.
 * O :app fornece; cada aba escuta com [ScrollToTopOnReselect].
 */
val LocalTabReselect = staticCompositionLocalOf<Flow<String>> { emptyFlow() }

/** Tocar de novo na aba [route] faz a lista [state] subir até o topo, com animação. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrollToTopOnReselect(route: String, state: LazyListState) {
    val events = LocalTabReselect.current
    val topBar = LocalTopBarState.current
    LaunchedEffect(events, state, route) {
        events.filter { it == route }.collect { state.animateScrollToItem(0) }
    }
    // De volta ao topo (por toque na aba ou de qualquer jeito): a barra de cima
    // volta à cor normal, sem ficar com o tom de "rolado".
    LaunchedEffect(state, topBar) {
        if (topBar == null) return@LaunchedEffect
        snapshotFlow { state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0 }
            .filter { it }
            .collect { topBar.contentOffset = 0f }
    }
}
