// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import com.boomlistener.core.designsystem.theme.boomScreenSpec
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
    val spec = boomScreenSpec<Float>()
    LaunchedEffect(events, state, route) {
        events.filter { it == route }.collect { state.scrollToTopSmoothly(spec) }
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

/**
 * Sobe até o topo num movimento só, sem paradas no caminho.
 *
 * `animateScrollToItem` com o alvo fora da tela anda em etapas (chuta uma
 * distância, mede, corrige), e cada etapa vira uma travadinha, ainda mais
 * passando por seções grandes como as do Início. Aqui a distância é conhecida
 * de antemão: se o topo está à vista, é a posição dele; se não, a lista pula
 * (sem animação) para exatamente uma tela abaixo do topo e anima esses pixels
 * de uma vez, com uma mola só.
 */
suspend fun LazyListState.scrollToTopSmoothly(spec: AnimationSpec<Float>) {
    val viewport = layoutInfo.viewportSize.height
    val firstVisible = layoutInfo.visibleItemsInfo.firstOrNull { it.index == 0 }
    if (firstVisible != null) {
        val distance = -firstVisible.offset
        if (distance > 0) animateScrollBy(-distance.toFloat(), spec)
        scrollToItem(0)
        return
    }
    if (viewport <= 0) {
        scrollToItem(0)
        return
    }
    scrollToItem(0, viewport)
    // A lista acaba antes (pouco conteúdo): o pulo foi encurtado. Mede de novo.
    val item0 = layoutInfo.visibleItemsInfo.firstOrNull { it.index == 0 }
    val distance = when {
        item0 != null -> -item0.offset
        firstVisibleItemIndex == 0 -> firstVisibleItemScrollOffset
        else -> viewport
    }
    if (distance > 0) animateScrollBy(-distance.toFloat(), spec)
    // Garante o topo exato (arredondamentos da mola).
    scrollToItem(0)
}
