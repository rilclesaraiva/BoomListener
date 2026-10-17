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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onOpenArtist: (artistName: String) -> Unit,
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

    // A fila mora aqui (e não dentro do conteúdo) porque, enquanto ela está
    // aberta, os gestos e o "voltar" pertencem só a ela.
    val queueState = rememberPlayerSheetState()
    LaunchedEffect(state.isVisible) {
        if (!state.isVisible) queueState.reset()
    }
    // Decidido no começo de cada arraste: com a fila aberta, arrastar em
    // qualquer ponto do player mexe só na fila, nunca fecha o player junto.
    var dragTargetsQueue by remember { mutableStateOf(false) }

    // Mede a tela sempre, mesmo fechado: o mini player precisa da altura para
    // o arraste para cima acompanhar o dedo na proporção certa.
    BoxWithConstraints(modifier.fillMaxSize()) {
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
        state.screenHeightPx = heightPx
        queueState.screenHeightPx = heightPx
        if (!state.isVisible) return@BoxWithConstraints

        NowPlayingContent(
            onCollapse = { if (queueState.isVisible) queueState.collapse() else state.collapse() },
            viewModel = viewModel,
            queueState = queueState,
            onOpenArtist = onOpenArtist,
            modifier = Modifier
                .graphicsLayer {
                    translationY = (1f - state.progress) * heightPx
                }
                .draggable(
                    state = rememberDraggableState { delta ->
                        if (dragTargetsQueue) queueState.dragBy(-delta) else state.dragBy(-delta)
                    },
                    orientation = Orientation.Vertical,
                    onDragStarted = { dragTargetsQueue = queueState.isVisible },
                    onDragStopped = { velocity ->
                        if (dragTargetsQueue) queueState.settle(-velocity) else state.settle(-velocity)
                    },
                ),
        )
    }
}
