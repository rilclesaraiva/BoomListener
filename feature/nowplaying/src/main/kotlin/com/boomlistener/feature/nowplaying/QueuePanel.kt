// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.boomlistener.core.designsystem.component.BoomArtwork
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.designsystem.component.BoomListArtworkSize
import com.boomlistener.core.player.PlaybackState
import com.boomlistener.core.player.QueueEntry
import kotlinx.coroutines.CancellationException

/**
 * A fila de reprodução, que sobe por cima do player. Fecha puxando o topo
 * para baixo, com a seta ou com o "voltar" do sistema (que, com a fila
 * aberta, fecha só a fila e não o player).
 */
@Composable
internal fun QueuePanel(
    state: PlaybackState,
    queueState: PlayerSheetState,
    viewModel: NowPlayingViewModel,
) {
    PredictiveBackHandler(enabled = queueState.isExpanded) { backEvents ->
        try {
            backEvents.collect { event -> queueState.snapTo(1f - event.progress * 0.35f) }
            queueState.collapse()
        } catch (cancelled: CancellationException) {
            queueState.expand()
            throw cancelled
        }
    }

    if (!queueState.isVisible) return

    val heightPx = queueState.screenHeightPx
    val listState = rememberLazyListState()
    val currentPosition = state.queue.indexOfFirst { it.index == state.queueIndex }

    // Ao abrir, a lista já começa na música que está tocando.
    LaunchedEffect(queueState.isExpanded) {
        if (queueState.isExpanded && currentPosition >= 0) listState.scrollToItem(currentPosition)
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.extraLarge.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize),
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 48.dp)
            .graphicsLayer { translationY = (1f - queueState.progress) * heightPx },
    ) {
        Column {
            // Cabeçalho: puxar para baixo fecha a fila.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .draggable(
                        state = rememberDraggableState { delta -> queueState.dragBy(-delta) },
                        orientation = Orientation.Vertical,
                        onDragStopped = { velocity -> queueState.settle(-velocity) },
                    )
                    .padding(top = 12.dp),
            ) {
                DragHandle()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.feature_nowplaying_up_next),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (state.queueSize > 0) {
                            Text(
                                text = stringResource(
                                    R.string.feature_nowplaying_queue_position,
                                    state.queueIndex + 1,
                                    state.queueSize,
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    IconButton(onClick = queueState::collapse) {
                        Icon(BoomIcons.Collapse, stringResource(R.string.feature_nowplaying_queue_close))
                    }
                }
            }

            LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                items(state.queue, key = { it.index }) { entry ->
                    QueueRow(
                        entry = entry,
                        isCurrent = entry.index == state.queueIndex,
                        viewModel = viewModel,
                        onClick = { viewModel.playQueueItem(entry.index) },
                    )
                }
                item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
            }
        }
    }
}

@Composable
private fun QueueRow(
    entry: QueueEntry,
    isCurrent: Boolean,
    viewModel: NowPlayingViewModel,
    onClick: () -> Unit,
) {
    val sizePx = with(LocalDensity.current) { BoomListArtworkSize.roundToPx() }
    val artwork = rememberArtwork(viewModel, entry.item.uri, sizePx)
    val unknownArtist = stringResource(R.string.feature_nowplaying_unknown_artist)

    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(
            containerColor = if (isCurrent) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
            headlineColor = if (isCurrent) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
            supportingColor = if (isCurrent) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        leadingContent = { BoomArtwork(image = artwork, fallbackText = entry.item.title) },
        headlineContent = { Text(entry.item.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                text = if (isCurrent) {
                    stringResource(R.string.feature_nowplaying_playing) + " • " + (entry.item.artist ?: unknownArtist)
                } else {
                    entry.item.artist ?: unknownArtist
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}
