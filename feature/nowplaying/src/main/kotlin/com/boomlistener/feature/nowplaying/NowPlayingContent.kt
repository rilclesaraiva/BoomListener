// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import com.boomlistener.data.library.FavoritesData
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.core.designsystem.component.BoomArtwork
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.designsystem.component.BoomPlaceholder
import com.boomlistener.core.common.ExternalLinks
import com.boomlistener.core.common.openUrl
import com.boomlistener.core.common.shareLink
import com.boomlistener.core.designsystem.component.BoomMenuAction
import com.boomlistener.core.designsystem.component.BoomMenuHeader
import com.boomlistener.core.designsystem.component.BoomMenuKind
import com.boomlistener.core.designsystem.component.BoomMoreMenu
import com.boomlistener.core.designsystem.component.BoomSeekBar
import com.boomlistener.core.designsystem.component.artworkTintedBackground
import com.boomlistener.core.designsystem.component.rememberArtworkColor
import com.boomlistener.core.designsystem.theme.boomEffectsSpec
import com.boomlistener.core.designsystem.theme.boomSpatialSpec
import com.boomlistener.core.player.PlaybackSettingsData
import com.boomlistener.core.player.PlaybackState
import com.boomlistener.core.player.RepeatMode
import kotlinx.coroutines.CancellationException

private val ArtworkSizeForLoading = 720.dp

/**
 * O conteúdo do player em tela cheia (o painel que desliza fica em
 * [PlayerSheet]). Embaixo fica a faixa "A seguir": tocar ou puxar para cima
 * abre a fila ([QueuePanel]) por cima do player.
 */
@Composable
internal fun NowPlayingContent(
    onCollapse: () -> Unit,
    viewModel: NowPlayingViewModel,
    queueState: PlayerSheetState,
    onOpenArtist: (artistName: String) -> Unit,
    onEditTags: (trackUri: String) -> Unit,
    onAddToPlaylist: (trackUri: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val item = state.item
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sizePx = with(LocalDensity.current) { ArtworkSizeForLoading.roundToPx() }
    val artwork = rememberArtwork(viewModel, item?.uri, sizePx)
    val artworkColor = rememberArtworkColor(artwork)
    val showLyrics by viewModel.showLyrics.collectAsStateWithLifecycle()
    val lyrics by viewModel.lyrics.collectAsStateWithLifecycle()
    // A posição da letra sobrevive a ir para a capa e voltar; música nova, do topo.
    val lyricsScroll = remember(item?.uri) { LyricsScroll() }
    val spatial = boomSpatialSpec<Float>()
    val effects = boomEffectsSpec<Float>()
    val deleteSong = rememberSongDeleter()
    val showDetails = rememberSongDetails()
    val playbackSettings = remember { PlaybackSettingsData.get(context.applicationContext) }
    val sleepTimer by playbackSettings.sleepTimer.collectAsStateWithLifecycle()
    val playbackPrefs by playbackSettings.prefs.collectAsStateWithLifecycle()
    var showSleep by remember { mutableStateOf(false) }
    var showSpeed by remember { mutableStateOf(false) }
    if (showSleep) SleepTimerSheet(playbackSettings, onDismiss = { showSleep = false })
    if (showSpeed) SpeedSheet(playbackSettings, onDismiss = { showSpeed = false })

    // Com a letra aberta, o "voltar" fecha só a letra (a capa volta com a
    // mesma animação), não o player inteiro. A fila, quando aberta, tem prioridade.
    PredictiveBackHandler(enabled = showLyrics && !queueState.isVisible) { backEvents ->
        try {
            backEvents.collect { }
            viewModel.toggleLyrics()
        } catch (cancelled: CancellationException) {
            throw cancelled
        }
    }

    Box(modifier.fillMaxSize()) {
        // Fundo tingido com a cor da capa da música atual.
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
          Box(Modifier.fillMaxSize().artworkTintedBackground(artworkColor)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp),
            ) {
                // Topo: minimizar + posição na fila.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                ) {
                    IconButton(onClick = onCollapse) {
                        Icon(BoomIcons.Collapse, stringResource(R.string.feature_nowplaying_collapse))
                    }
                    // O "x de y" agora fica na fila; o topo só tem os botões.
                    Spacer(Modifier.weight(1f))
                    DevicesButton()
                    if (item != null) {
                        BoomMoreMenu(
                            actions = listOfNotNull(
                                // Letra: o mesmo que tocar na capa.
                                BoomMenuAction(
                                    stringResource(
                                        if (showLyrics) R.string.feature_nowplaying_hide_lyrics
                                        else R.string.feature_nowplaying_show_lyrics,
                                    ),
                                    kind = BoomMenuKind.Lyrics,
                                ) { viewModel.toggleLyrics() },
                                // Mix: enche a fila com o resto da biblioteca, misturado.
                                BoomMenuAction(stringResource(R.string.feature_nowplaying_mix), kind = BoomMenuKind.Mix) { viewModel.mix() },
                                BoomMenuAction(stringResource(R.string.feature_nowplaying_queue_open), kind = BoomMenuKind.Queue) { queueState.expand() },
                                BoomMenuAction(sleepTimerLabel(sleepTimer), kind = BoomMenuKind.SleepTimer) { showSleep = true },
                                BoomMenuAction(
                                    stringResource(R.string.feature_nowplaying_speed) + " · " + formatSpeed(playbackPrefs.speed),
                                    kind = BoomMenuKind.Speed,
                                ) { showSpeed = true },
                                BoomMenuAction(stringResource(R.string.feature_nowplaying_add_to_playlist), kind = BoomMenuKind.AddToPlaylist) {
                                    onAddToPlaylist(item.uri)
                                },
                                item.artist?.let { name ->
                                    BoomMenuAction(stringResource(R.string.feature_nowplaying_go_to_artist), kind = BoomMenuKind.GoToArtist) { onOpenArtist(name) }
                                },
                                BoomMenuAction(stringResource(R.string.feature_nowplaying_edit_tags), kind = BoomMenuKind.EditTags) { onEditTags(item.uri) },
                                BoomMenuAction(stringResource(R.string.feature_nowplaying_details), kind = BoomMenuKind.Details) {
                                    showDetails(item.uri, item.title)
                                },
                                BoomMenuAction(stringResource(R.string.feature_nowplaying_delete_song), kind = BoomMenuKind.Delete) {
                                    deleteSong(item.uri, item.title)
                                },
                            ),
                            header = BoomMenuHeader(item.title, item.artist, artwork),
                            onShare = {
                                scope.launch {
                                    context.shareLink(
                                        url = viewModel.shareUrl(item),
                                        title = listOfNotNull(item.title, item.artist).joinToString(" — "),
                                    )
                                }
                            },
                            onYoutubeMusic = { context.openUrl(ExternalLinks.youtubeMusicSearch(item.title, item.artist)) },
                        )
                    }
                }

                if (item == null) {
                    BoomPlaceholder(
                        title = stringResource(R.string.feature_nowplaying_empty_title),
                        message = stringResource(R.string.feature_nowplaying_message),
                    )
                    return@Column
                }

                // Capa ⇄ letra: tocar na capa abre a letra, com a capa encolhendo
                // e esmaecendo enquanto a letra entra.
                val artworkArea: @Composable (Modifier) -> Unit = { areaModifier ->
                    AnimatedContent(
                        targetState = showLyrics,
                        transitionSpec = {
                            (fadeIn(effects) + scaleIn(spatial, initialScale = 0.92f)) togetherWith
                                (fadeOut(effects) + scaleOut(spatial, targetScale = 0.92f))
                        },
                        label = "artworkOrLyrics",
                        modifier = areaModifier,
                    ) { lyricsOn ->
                        if (lyricsOn) {
                            LyricsView(
                                state = lyrics,
                                positionMs = state.positionMs,
                                onSeek = viewModel::seekTo,
                                onRetry = viewModel::retryLyrics,
                                // Tocar na letra volta para a capa, como tocar na capa abriu a letra.
                                onTap = viewModel::toggleLyrics,
                                scroll = lyricsScroll,
                            )
                        } else {
                            // Deslizar a capa para os lados troca de música: a próxima
                            // (ou a anterior) já aparece ao lado enquanto o dedo puxa.
                            QueuePager(
                                state = state,
                                onSkipTo = viewModel::skipToQueueItem,
                                pageSpacing = 24.dp,
                                modifier = Modifier.fillMaxSize(),
                            ) { song ->
                                // Cada página carrega a própria capa (usar a da música atual
                                // fazia a capa anterior piscar na página nova ao trocar).
                                val cover = rememberArtwork(viewModel, song.uri, sizePx)
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    BoomArtwork(
                                        image = cover,
                                        fallbackText = song.title,
                                        size = null,
                                        // Quadrada no maior tamanho que cabe (em pé, a largura;
                                        // deitado, a altura).
                                        modifier = Modifier
                                            .aspectRatio(1f, matchHeightConstraintsFirst = true)
                                            .clickable(onClick = viewModel::toggleLyrics),
                                    )
                                }
                            }
                        }
                    }
                }
                val details: @Composable () -> Unit = {
                    Spacer(Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    // Tocar no nome do artista abre o perfil dele.
                    Text(
                        text = listOfNotNull(
                            item.artist ?: stringResource(R.string.feature_nowplaying_unknown_artist),
                            item.album,
                        ).joinToString(" • "),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.then(
                            item.artist?.let { name -> Modifier.clickable { onOpenArtist(name) } } ?: Modifier,
                        ),
                    )
                    }
                    FavoriteButton(item.uri)
                    }

                    Spacer(Modifier.height(24.dp))
                    // Arrastando a bolinha: o balão mostra onde ela está.
                    BoomSeekBar(
                        progress = state.progress,
                        isPlaying = state.isPlaying,
                        onSeek = viewModel::seekToFraction,
                        // O tempo do alvo fica no balão; o da esquerda segue a música.
                        dragLabel = { fraction -> formatTime((fraction * state.durationMs).toLong()) },
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatTime(state.positionMs), style = MaterialTheme.typography.labelMedium)
                        Text(formatTime(state.durationMs), style = MaterialTheme.typography.labelMedium)
                    }

                    Spacer(Modifier.height(16.dp))
                    Controls(
                        isPlaying = state.isPlaying,
                        shuffle = state.shuffle,
                        repeatMode = state.repeatMode,
                        onShuffle = viewModel::toggleShuffle,
                        onPrevious = viewModel::previous,
                        onPlayPause = viewModel::togglePlayPause,
                        onNext = viewModel::next,
                        onRepeat = viewModel::cycleRepeatMode,
                    )
                    Spacer(Modifier.height(4.dp))
                    UpNextHandle(state = state, queueState = queueState)
                    // Folga para o puxador da fila não ficar colado no gesto de
                    // "ir para a tela inicial" do Android, que fica logo abaixo.
                    Spacer(Modifier.height(20.dp))
                }
                // Na horizontal (celular deitado, tablet): capa à esquerda, o resto à direita.
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val wide = maxWidth > maxHeight && maxWidth >= 560.dp
                    if (wide) {
                        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                            artworkArea(Modifier.weight(1f).fillMaxHeight())
                            Spacer(Modifier.width(32.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) { details() }
                        }
                    } else {
                        Column(Modifier.fillMaxSize()) {
                            artworkArea(Modifier.weight(1f).fillMaxWidth())
                            details()
                        }
                    }
                }
            }
          }
        }

        QueuePanel(
            state = state,
            queueState = queueState,
            viewModel = viewModel,
            onOpenArtist = onOpenArtist,
            onEditTags = onEditTags,
            onAddToPlaylist = onAddToPlaylist,
            onDelete = deleteSong,
        )
    }
}

/**
 * Faixa de baixo do player: um "puxador" e a próxima música. Tocar ou
 * puxar para cima abre a fila, acompanhando o dedo.
 */
@Composable
private fun UpNextHandle(state: PlaybackState, queueState: PlayerSheetState) {
    val currentPosition = state.queue.indexOfFirst { it.index == state.queueIndex }
    val next = state.queue.getOrNull(currentPosition + 1)?.item
    val openQueue = stringResource(R.string.feature_nowplaying_queue_open)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .semantics { contentDescription = openQueue }
            .clickable(onClick = queueState::expand)
            .draggable(
                state = rememberDraggableState { delta -> queueState.dragBy(-delta) },
                orientation = Orientation.Vertical,
                onDragStopped = { velocity -> queueState.settle(-velocity) },
            )
            .padding(vertical = 12.dp),
    ) {
        DragHandle()
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (next != null) {
                stringResource(R.string.feature_nowplaying_up_next_song, next.title)
            } else {
                stringResource(R.string.feature_nowplaying_queue_end)
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Barrinha horizontal que indica "dá para puxar". */
@Composable
internal fun DragHandle(modifier: Modifier = Modifier) {
    Box(
        modifier
            .width(32.dp)
            .height(4.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
    )
}

@Composable
private fun Controls(
    isPlaying: Boolean,
    shuffle: Boolean,
    repeatMode: RepeatMode,
    onShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
) {
    // Aleatório e repetir viram botões "apertados" (fundo preenchido) quando
    // ligados, para ficar óbvio que estão ativos.
    val toggleColors = IconButtonDefaults.iconToggleButtonColors(
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        checkedContainerColor = MaterialTheme.colorScheme.primary,
        checkedContentColor = MaterialTheme.colorScheme.onPrimary,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconToggleButton(
            checked = shuffle,
            onCheckedChange = { onShuffle() },
            colors = toggleColors,
        ) {
            Icon(BoomIcons.Shuffle, stringResource(R.string.feature_nowplaying_shuffle))
        }
        FilledTonalIconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
            Icon(BoomIcons.SkipPrevious, stringResource(R.string.feature_nowplaying_previous))
        }
        FilledIconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(80.dp),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            if (isPlaying) {
                Icon(BoomIcons.Pause, stringResource(R.string.feature_nowplaying_pause), Modifier.size(36.dp))
            } else {
                Icon(BoomIcons.Play, stringResource(R.string.feature_nowplaying_play), Modifier.size(36.dp))
            }
        }
        FilledTonalIconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
            Icon(BoomIcons.SkipNext, stringResource(R.string.feature_nowplaying_next))
        }
        IconToggleButton(
            checked = repeatMode != RepeatMode.Off,
            onCheckedChange = { onRepeat() },
            colors = toggleColors,
        ) {
            Icon(
                if (repeatMode == RepeatMode.One) BoomIcons.RepeatOne else BoomIcons.Repeat,
                stringResource(R.string.feature_nowplaying_repeat),
            )
        }
    }
}

/** 215000 ms → "3:35". */
internal fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}

/** Coração da música que está tocando: favoritar ou tirar dos favoritos. */
@Composable
private fun FavoriteButton(uri: String) {
    val context = LocalContext.current.applicationContext
    val repository = remember { FavoritesData.repository(context) }
    val favorites by repository.favorites.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val favorite = uri in favorites
    IconButton(onClick = { scope.launch { repository.toggle(uri) } }) {
        Icon(
            imageVector = if (favorite) BoomIcons.Favorite else BoomIcons.FavoriteBorder,
            contentDescription = stringResource(if (favorite) R.string.feature_nowplaying_unfavorite else R.string.feature_nowplaying_favorite),
            tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
