// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.boomlistener.core.designsystem.component.BoomThinProgress
import com.boomlistener.core.designsystem.theme.boomEffectsSpec
import com.boomlistener.core.designsystem.theme.boomFastSpatialSpec
import com.boomlistener.core.designsystem.theme.boomSpatialSpec
import kotlinx.coroutines.launch

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
    val state by viewModel.state.collectAsStateWithLifecycle()
    val item = state.item
    val sizePx = with(LocalDensity.current) { 48.dp.roundToPx() }
    val artwork = rememberArtwork(viewModel, item?.uri, sizePx)

    val scope = rememberCoroutineScope()
    val dismissSpec = boomSpatialSpec<Float>()
    val returnSpec = boomFastSpatialSpec<Float>()
    // Quanto o mini player foi puxado para baixo (px), no gesto de encerrar.
    val dismissOffset = remember { Animatable(0f) }
    var heightPx by remember { mutableFloatStateOf(1f) }

    AnimatedVisibility(
        visible = item != null,
        enter = expandVertically(boomSpatialSpec()) + fadeIn(boomEffectsSpec()),
        exit = shrinkVertically(boomSpatialSpec()) + fadeOut(boomEffectsSpec()),
        modifier = modifier,
    ) {
        val dragState = rememberDraggableState { delta ->
            val pullingDown = dismissOffset.value > 0f || (delta > 0f && sheetState.progress.value == 0f)
            if (pullingDown) {
                scope.launch { dismissOffset.snapTo((dismissOffset.value + delta).coerceAtLeast(0f)) }
            } else {
                sheetState.dragBy(-delta)
            }
        }
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .onSizeChanged { heightPx = it.height.toFloat().coerceAtLeast(1f) }
                .graphicsLayer {
                    translationY = dismissOffset.value
                    alpha = 1f - (dismissOffset.value / heightPx).coerceIn(0f, 1f) * 0.8f
                }
                .draggable(
                    state = dragState,
                    orientation = Orientation.Vertical,
                    onDragStopped = { velocity ->
                        if (dismissOffset.value > 0f) {
                            val dismiss = dismissOffset.value > heightPx * 0.4f || velocity > 1500f
                            if (dismiss) {
                                dismissOffset.animateTo(heightPx * 1.5f, dismissSpec)
                                viewModel.stop()
                                dismissOffset.snapTo(0f)
                            } else {
                                dismissOffset.animateTo(0f, returnSpec)
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
                    modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 6.dp),
                )
            }
        }
    }
}
