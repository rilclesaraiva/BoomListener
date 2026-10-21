// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.boomlistener.core.player.NowPlayingItem
import com.boomlistener.core.player.PlaybackState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs

/**
 * Deslizar para os lados troca de música, como numa fila de cartões: cada
 * página é uma música da fila, na ordem em que vai tocar. Enquanto o dedo
 * arrasta, a capa da próxima (ou da anterior) já aparece ao lado; ao soltar
 * passando do meio, a música troca na hora. Usado no mini player e na capa do
 * player aberto. Esquerda = próxima, direita = anterior.
 *
 * Trocar de música por outro caminho (botões, fim da música, fone) leva o
 * carrossel junto, com animação quando é a vizinha.
 */
@Composable
internal fun QueuePager(
    state: PlaybackState,
    onSkipTo: (queueIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    pageSpacing: Dp = 0.dp,
    page: @Composable (NowPlayingItem) -> Unit,
) {
    val entries = state.queue
    val current = entries.indexOfFirst { it.index == state.queueIndex }
    // Fila ainda não chegou (ou só uma música): sem carrossel.
    if (current < 0 || entries.size < 2) {
        state.item?.let { page(it) }
        return
    }
    val pagerState = rememberPagerState(initialPage = current) { entries.size }
    val latestCurrent by rememberUpdatedState(current)
    val latestEntries by rememberUpdatedState(entries)
    val latestSkip by rememberUpdatedState(onSkipTo)

    // A música mudou por fora: o carrossel acompanha (sem brigar com o dedo).
    LaunchedEffect(current, entries.size) {
        if (pagerState.isScrollInProgress || pagerState.currentPage == current) return@LaunchedEffect
        if (abs(pagerState.currentPage - current) == 1) pagerState.animateScrollToPage(current) else pagerState.scrollToPage(current)
    }
    // Parou numa música diferente da que toca: troca.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { settled ->
                if (settled != latestCurrent) latestEntries.getOrNull(settled)?.let { latestSkip(it.index) }
            }
    }

    HorizontalPager(
        state = pagerState,
        key = { entries[it].key },
        pageSpacing = pageSpacing,
        // A vizinha já fica pronta (capa carregada) antes de o dedo puxar.
        beyondViewportPageCount = 1,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) { index ->
        entries.getOrNull(index)?.let { page(it.item) }
    }
}
