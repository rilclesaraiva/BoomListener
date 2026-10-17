// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
@Composable
fun ScrollToTopOnReselect(route: String, state: LazyListState) {
    val events = LocalTabReselect.current
    LaunchedEffect(events, state, route) {
        events.filter { it == route }.collect { state.animateScrollToItem(0) }
    }
}
