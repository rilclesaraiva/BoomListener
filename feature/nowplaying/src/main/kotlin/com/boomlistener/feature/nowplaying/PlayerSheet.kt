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
import com.boomlistener.core.designsystem.component.artworkTintedBackground
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Size
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
    onEditTags: (trackUri: String) -> Unit,
    onAddToPlaylist: (trackUri: String) -> Unit,
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
    //
    // O player cresce a partir do mini player: fechado, ele é um cartão do
    // tamanho do mini player (mesmas margens e cantos); abrindo, sobe até o
    // topo, perde as margens e os cantos, a barra de abas desce (no :app,
    // por [PlayerSheetState.fraction]) e o conteúdo do mini player esmaece
    // enquanto o da tela cheia aparece.
    val corner = MaterialTheme.shapes.extraLarge.topStart
    val density = LocalDensity.current
    val sideInsetPx = with(density) { MiniPlayerSideInset.toPx() }
    val topInsetPx = with(density) { MiniPlayerTopInset.toPx() }
    // O cartão tem o fundo do player aberto (tom da capa), igual ao mini player.
    val tint = rememberPlayerTint(viewModel, playback.item?.uri)
    val surface = MaterialTheme.colorScheme.surface
    BoxWithConstraints(modifier.fillMaxSize()) {
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
        val cornerPx = corner.toPx(Size(heightPx, heightPx), density)
        state.screenHeightPx = heightPx
        queueState.screenHeightPx = heightPx
        if (!state.isVisible) return@BoxWithConstraints

        // Tudo lido dentro do graphicsLayer: acompanhar o dedo não recompõe a tela.
        val growLayer: GraphicsLayerScope.() -> Unit = {
            val p = state.progress
            val closed = (1f - p).coerceIn(0f, 1f)
            // A mola passa um pouco do fim: o player não sobe além do topo.
            val offset = (1f - p).coerceAtLeast(0f) * state.travelPx
            translationY = offset
            // A base também cresce, do fim do mini player até o fim da tela,
            // empurrando a barra de abas para baixo.
            val miniBottom = if (state.collapsedBottomPx > 0f) state.collapsedBottomPx else heightPx
            val bottomOnScreen = miniBottom + (heightPx - miniBottom) * p.coerceIn(0f, 1f)
            shape = InsetRoundedShape(
                sidePx = sideInsetPx * closed,
                topPx = topInsetPx * closed,
                bottomPx = (bottomOnScreen - offset - topInsetPx * closed).coerceAtLeast(topInsetPx * closed + 1f),
                radiusPx = cornerPx * (closed * 2f).coerceIn(0f, 1f),
            )
            clip = true
        }

        // Fundo do cartão que cresce (aparece antes do conteúdo da tela cheia).
        Box(Modifier.fillMaxSize().graphicsLayer(growLayer).background(surface).artworkTintedBackground(tint))

        NowPlayingContent(
            onCollapse = { if (queueState.isVisible) queueState.collapse() else state.collapse() },
            viewModel = viewModel,
            queueState = queueState,
            onOpenArtist = onOpenArtist,
            onEditTags = onEditTags,
            onAddToPlaylist = onAddToPlaylist,
            modifier = Modifier
                .graphicsLayer {
                    growLayer()
                    // A tela cheia surge depois que o mini player já esmaeceu.
                    alpha = ((state.progress - FULL_FADE_START) / FULL_FADE_LENGTH).coerceIn(0f, 1f)
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

        // O mini player no começo do cartão, esmaecendo enquanto ele cresce
        // (e voltando ao fechar). Some de vez quando já não se vê.
        if (state.progress < MINI_FADE_LENGTH) {
            MiniPlayer(
                sheetState = state,
                modifier = Modifier.graphicsLayer {
                    translationY = (1f - state.progress).coerceAtLeast(0f) * state.travelPx
                    alpha = (1f - state.progress / MINI_FADE_LENGTH).coerceIn(0f, 1f)
                },
            )
        }
    }
}

/** Retângulo de cantos arredondados com margens dos lados e em cima, cortado em [bottomPx]. */
private class InsetRoundedShape(
    private val sidePx: Float,
    private val topPx: Float,
    private val bottomPx: Float,
    private val radiusPx: Float,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Rounded(
            RoundRect(
                left = sidePx,
                top = topPx,
                right = (size.width - sidePx).coerceAtLeast(sidePx),
                bottom = bottomPx.coerceAtMost(size.height),
                cornerRadius = CornerRadius(radiusPx, radiusPx),
            ),
        )
}

/** Margens do mini player (as do cartão dele no MiniPlayer). */
private val MiniPlayerSideInset = 8.dp
private val MiniPlayerTopInset = 4.dp

private const val MINI_FADE_LENGTH = 0.3f
private const val FULL_FADE_START = 0.2f
private const val FULL_FADE_LENGTH = 0.45f
