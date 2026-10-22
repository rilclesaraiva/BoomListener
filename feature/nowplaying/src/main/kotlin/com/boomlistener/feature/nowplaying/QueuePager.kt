// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import android.os.SystemClock
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
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
    // A música para onde o dedo levou, até o player confirmar a troca. Nesse
    // meio-tempo o player às vezes ainda informa a música anterior; sem isso o
    // carrossel voltava para ela por um instante (a "piscada").
    var pending by remember { mutableStateOf<Pending?>(null) }

    // A música mudou por fora (ou a fila foi trocada): o carrossel acompanha,
    // sem brigar com o dedo.
    LaunchedEffect(current, entries) {
        val waiting = pending
        if (waiting != null) {
            val confirmed = state.queueIndex == waiting.queueIndex
            val expired = SystemClock.uptimeMillis() - waiting.sinceMs > PENDING_TIMEOUT_MS
            if (!confirmed && !expired) return@LaunchedEffect
            pending = null
        }
        if (pagerState.isScrollInProgress || pagerState.currentPage == current) return@LaunchedEffect
        if (abs(pagerState.currentPage - current) == 1) pagerState.animateScrollToPage(current) else pagerState.scrollToPage(current)
    }
    // Só o dedo troca de música por aqui. Antes, qualquer parada (inclusive a
    // do próprio carrossel se ajeitando numa fila nova, como ao tocar no
    // aleatório) chamava a troca e fazia a música anterior voltar a tocar.
    LaunchedEffect(pagerState) {
        var dragged = false
        launch {
            pagerState.interactionSource.interactions.collect { if (it is DragInteraction.Start) dragged = true }
        }
        snapshotFlow { pagerState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling ->
                if (scrolling || !dragged) return@collect
                dragged = false
                val settled = pagerState.currentPage
                if (settled == latestCurrent) return@collect
                latestEntries.getOrNull(settled)?.let { entry ->
                    pending = Pending(entry.index, SystemClock.uptimeMillis())
                    latestSkip(entry.index)
                }
            }
    }

    HorizontalPager(
        state = pagerState,
        // Sem "key" no carrossel: com ela, trocar a fila inteira fazia o pager
        // pular para onde a música antiga foi parar na fila nova. O estado de
        // cada página (a capa carregada) fica preso à música pelo key() abaixo.
        pageSpacing = pageSpacing,
        // A vizinha já fica pronta (capa carregada) antes de o dedo puxar.
        beyondViewportPageCount = 1,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) { index ->
        entries.getOrNull(index)?.let { entry -> key(entry.key) { page(entry.item) } }
    }
}

private class Pending(val queueIndex: Int, val sinceMs: Long)

/** Tempo máximo esperando o player confirmar a troca feita pelo dedo. */
private const val PENDING_TIMEOUT_MS = 1_500L
