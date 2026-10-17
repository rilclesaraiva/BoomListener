// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException

/**
 * Player em tela cheia como painel: sobe por cima do app (inclusive da barra
 * de navegação) e desce com o dedo, com o "voltar" do sistema ou com a seta.
 * Fica no :app por cima de tudo; o mini player controla a abertura.
 */
@Composable
fun PlayerSheet(
    state: PlayerSheetState,
    modifier: Modifier = Modifier,
) {
    val viewModel = nowPlayingViewModel()
    val playback by viewModel.state.collectAsStateWithLifecycle()

    // Fila esvaziou (música encerrada pelo mini player): fecha o painel.
    LaunchedEffect(playback.item == null) {
        if (playback.item == null) state.collapse()
    }

    // "Voltar" do sistema com o painel aberto: o painel acompanha o dedo
    // (voltar preditivo) e fecha ao soltar; se o gesto for cancelado, reabre.
    PredictiveBackHandler(enabled = state.isExpanded) { backEvents ->
        try {
            backEvents.collect { event -> state.snapTo(1f - event.progress * 0.35f) }
            state.collapse()
        } catch (cancelled: CancellationException) {
            state.expand()
            throw cancelled
        }
    }

    // Mede a tela sempre, mesmo fechado: o mini player precisa da altura para
    // o arraste para cima acompanhar o dedo na proporção certa.
    BoxWithConstraints(modifier.fillMaxSize()) {
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
        state.screenHeightPx = heightPx
        if (!state.isVisible) return@BoxWithConstraints

        NowPlayingContent(
            onCollapse = state::collapse,
            viewModel = viewModel,
            modifier = Modifier
                .graphicsLayer {
                    translationY = (1f - state.progress) * heightPx
                }
                .draggable(
                    state = rememberDraggableState { delta -> state.dragBy(-delta) },
                    orientation = Orientation.Vertical,
                    onDragStopped = { velocity -> state.settle(-velocity) },
                ),
        )
    }
}
