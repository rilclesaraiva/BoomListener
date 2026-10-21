// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.boomlistener.core.designsystem.component.BoomArtwork
import com.boomlistener.core.designsystem.component.BoomFastScroller
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.designsystem.component.BoomListArtworkSize
import com.boomlistener.core.designsystem.component.BoomMenuAction
import com.boomlistener.core.designsystem.component.BoomMoreMenu
import com.boomlistener.core.designsystem.component.clickableWithMenu
import com.boomlistener.core.designsystem.component.trackPress
import com.boomlistener.core.player.PlaybackState
import com.boomlistener.core.player.QueueEntry
import kotlinx.coroutines.CancellationException
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

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
    onOpenArtist: (String) -> Unit,
    onEditTags: (String) -> Unit,
    onAddToPlaylist: (String) -> Unit,
    onDelete: (uri: String, title: String) -> Unit,
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

    // Cópia local da fila: enquanto o dedo arrasta uma música, a lista muda na
    // hora aqui na tela, e o player só recebe a mudança ao soltar.
    var localQueue by remember { mutableStateOf(state.queue) }
    var dragging by remember { mutableStateOf(false) }
    var dragStartPosition by remember { mutableIntStateOf(-1) }
    LaunchedEffect(state.queue) { if (!dragging) localQueue = state.queue }

    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        if (from.index in localQueue.indices && to.index in localQueue.indices) {
            localQueue = localQueue.toMutableList().apply { add(to.index, removeAt(from.index)) }
        }
    }
    val currentPosition = localQueue.indexOfFirst { it.index == state.queueIndex }

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
                        // Posição na fila ("9 de 72"), que antes ficava no topo do player.
                        Text(
                            text = if (state.queueSize > 0) {
                                stringResource(R.string.feature_nowplaying_queue_position, state.queueIndex + 1, state.queueSize)
                            } else {
                                stringResource(R.string.feature_nowplaying_reorder)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = queueState::collapse) {
                        Icon(BoomIcons.Collapse, stringResource(R.string.feature_nowplaying_queue_close))
                    }
                }
            }

            Box(Modifier.weight(1f)) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                items(localQueue, key = { it.key }) { entry ->
                    ReorderableItem(reorderState, key = entry.key) { isDragging ->
                        val isCurrent = entry.index == state.queueIndex
                        Surface(shadowElevation = if (isDragging) 8.dp else 0.dp) {
                            QueueRow(
                                entry = entry,
                                isCurrent = isCurrent,
                                viewModel = viewModel,
                                onClick = { viewModel.playQueueItem(entry.index) },
                                // Puxador ao lado da capa: segurar e arrastar
                                // move a música para qualquer ponto da fila.
                                dragHandle = {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .longPressDraggableHandle(
                                                onDragStarted = {
                                                    dragging = true
                                                    dragStartPosition = localQueue.indexOfFirst { it.key == entry.key }
                                                },
                                                onDragStopped = {
                                                    dragging = false
                                                    val end = localQueue.indexOfFirst { it.key == entry.key }
                                                    if (dragStartPosition >= 0 && end >= 0) {
                                                        // A fila na tela é a ordem real do player.
                                                        viewModel.moveQueueItem(dragStartPosition, end)
                                                    }
                                                },
                                            ),
                                    ) {
                                        Icon(BoomIcons.DragHandle, stringResource(R.string.feature_nowplaying_reorder))
                                    }
                                },
                                // Toda música tem o menu (a que toca também); remover fica
                                // nele, longe do toque sem querer.
                                menuActions = listOfNotNull(
                                    if (!isCurrent) {
                                        BoomMenuAction(stringResource(R.string.feature_nowplaying_play_now)) {
                                            viewModel.playQueueItem(entry.index)
                                        }
                                    } else {
                                        null
                                    },
                                    if (!isCurrent) {
                                        BoomMenuAction(stringResource(R.string.feature_nowplaying_remove)) {
                                            viewModel.removeQueueItem(entry.index)
                                        }
                                    } else {
                                        null
                                    },
                                    BoomMenuAction(stringResource(R.string.feature_nowplaying_add_to_playlist)) { onAddToPlaylist(entry.item.uri) },
                                    favoriteAction(entry.item.uri),
                                    entry.item.artist?.let { name ->
                                        BoomMenuAction(stringResource(R.string.feature_nowplaying_go_to_artist)) { onOpenArtist(name) }
                                    },
                                    BoomMenuAction(stringResource(R.string.feature_nowplaying_edit_tags)) { onEditTags(entry.item.uri) },
                                    BoomMenuAction(stringResource(R.string.feature_nowplaying_delete_song)) {
                                        onDelete(entry.item.uri, entry.item.title)
                                    },
                                ),
                            )
                        }
                    }
                }
                item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
            }
            // Arrastar pela direita pula pela fila, mostrando a letra.
            BoomFastScroller(
                state = listState,
                // Sem balão de letra: a fila tem a ordem de tocar (e dá para mudar), não a alfabética.
                label = { null },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
            )
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
    dragHandle: @Composable () -> Unit,
    menuActions: List<BoomMenuAction>,
) {
    val menuOpen = remember { mutableStateOf(false) }
    // Dedo no puxador = arrastar; aí segurar não abre o menu.
    val handlePressed = remember { mutableStateOf(false) }
    val sizePx = with(LocalDensity.current) { BoomListArtworkSize.roundToPx() }
    val artwork = rememberArtwork(viewModel, entry.item.uri, sizePx)
    val unknownArtist = stringResource(R.string.feature_nowplaying_unknown_artist)

    ListItem(
        // Segurar abre o mesmo menu dos três pontinhos.
        modifier = Modifier.clickableWithMenu(menuOpen, longPressEnabled = { !handlePressed.value }, onClick = onClick),
        colors = ListItemDefaults.colors(
            containerColor = if (isCurrent) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
            headlineColor = if (isCurrent) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
            supportingColor = if (isCurrent) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        leadingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.trackPress(handlePressed)) { dragHandle() }
                BoomArtwork(image = artwork, fallbackText = entry.item.title)
            }
        },
        trailingContent = { BoomMoreMenu(actions = menuActions, openState = menuOpen) },
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
