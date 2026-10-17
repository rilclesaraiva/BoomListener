// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.core.designsystem.component.BoomArtwork
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.designsystem.component.BoomArtworkSurface
import com.boomlistener.core.designsystem.component.BoomThinProgress
import com.boomlistener.core.designsystem.component.rememberArtworkColor
import com.boomlistener.core.designsystem.theme.boomEffectsSpec
import com.boomlistener.core.designsystem.theme.boomFastSpatialSpec
import com.boomlistener.core.designsystem.theme.boomSpatialSpec

/**
 * Mini player acima da barra de navegação. Só aparece quando há música na fila.
 *
 * Gestos: tocar ou puxar para cima abre o player em tela cheia; puxar para
 * baixo encerra a reprodução (a música para e o mini player some).
 */
@Composable
fun MiniPlayer(
    sheetState: PlayerSheetState,
    modifier: Modifier = Modifier,
) {
    val viewModel = nowPlayingViewModel()
    val liveState by viewModel.state.collectAsStateWithLifecycle()

    // Ao encerrar a música, a fila esvazia na hora, mas o mini player ainda
    // está saindo com animação. Sem isto ele piscaria vazio ("Artista
    // desconhecido", capa em branco, botão de tocar) nesse meio-tempo; com
    // isto ele sai mostrando a última música, como estava.
    var lastShownState by remember { mutableStateOf(liveState) }
    SideEffect { if (liveState.item != null) lastShownState = liveState }
    val state = if (liveState.item != null) liveState else lastShownState
    val item = state.item
    val visible = liveState.item != null
    val sizePx = with(LocalDensity.current) { 48.dp.roundToPx() }
    val artwork = rememberArtwork(viewModel, item?.uri, sizePx)
    // O mini player ganha o tom pastel da capa, para se destacar da barra de baixo.
    val artworkColor = rememberArtworkColor(artwork)

    val returnSpec = boomFastSpatialSpec<Float>()
    // Quanto o mini player foi puxado para baixo (px), no gesto de encerrar.
    // É estado simples, atualizado na hora a cada movimento do dedo.
    var dismissOffset by remember { mutableFloatStateOf(0f) }
    var heightPx by remember { mutableFloatStateOf(1f) }

    // Só volta o mini player para o lugar quando algo começar a tocar de novo
    // (antes disso ele ainda está saindo, e voltar agora causaria um salto).
    LaunchedEffect(visible) {
        if (visible) dismissOffset = 0f
    }

    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(boomSpatialSpec()) + fadeIn(boomEffectsSpec()),
        exit = shrinkVertically(boomSpatialSpec()) + fadeOut(boomEffectsSpec()),
        modifier = modifier,
    ) {
        val dragState = rememberDraggableState { delta ->
            val pullingDown = dismissOffset > 0f || (delta > 0f && sheetState.progress == 0f)
            if (pullingDown) {
                dismissOffset = (dismissOffset + delta).coerceAtLeast(0f)
            } else {
                sheetState.dragBy(-delta)
            }
        }
        BoomArtworkSurface(
            seed = artworkColor,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .onSizeChanged { heightPx = it.height.toFloat().coerceAtLeast(1f) }
                .graphicsLayer {
                    translationY = dismissOffset
                    alpha = 1f - (dismissOffset / heightPx).coerceIn(0f, 1f) * 0.8f
                }
                .draggable(
                    state = dragState,
                    orientation = Orientation.Vertical,
                    onDragStopped = { velocity ->
                        if (dismissOffset > 0f) {
                            val dismiss = dismissOffset > heightPx * 0.4f || velocity > 1500f
                            if (dismiss) {
                                // Primeiro encerra a música de verdade; depois
                                // termina de levar o mini player para baixo
                                // enquanto ele encolhe e some.
                                viewModel.stop()
                                animate(dismissOffset, heightPx, animationSpec = returnSpec) { value, _ ->
                                    dismissOffset = value
                                }
                            } else {
                                animate(dismissOffset, 0f, animationSpec = returnSpec) { value, _ ->
                                    dismissOffset = value
                                }
                            }
                        } else {
                            sheetState.settle(-velocity)
                        }
                    },
                )
                .clickable(onClick = sheetState::expand),
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 6.dp),
                ) {
                    BoomArtwork(image = artwork, fallbackText = item?.title.orEmpty(), size = 48.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                        Text(
                            text = item?.title.orEmpty(),
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = item?.artist ?: stringResource(R.string.feature_nowplaying_unknown_artist),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = viewModel::previous) {
                        Icon(BoomIcons.SkipPrevious, stringResource(R.string.feature_nowplaying_previous))
                    }
                    FilledIconButton(onClick = viewModel::togglePlayPause) {
                        if (state.isPlaying) {
                            Icon(BoomIcons.Pause, stringResource(R.string.feature_nowplaying_pause))
                        } else {
                            Icon(BoomIcons.Play, stringResource(R.string.feature_nowplaying_play))
                        }
                    }
                    IconButton(onClick = viewModel::next) {
                        Icon(BoomIcons.SkipNext, stringResource(R.string.feature_nowplaying_next))
                    }
                }
                BoomThinProgress(
                    progress = state.progress,
                    color = LocalContentColor.current,
                    modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 6.dp),
                )
            }
        }
    }
}
