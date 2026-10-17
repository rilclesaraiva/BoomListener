// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.core.designsystem.component.BoomArtwork
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.designsystem.component.BoomPlaceholder
import com.boomlistener.core.designsystem.component.BoomSeekBar
import com.boomlistener.core.player.RepeatMode

private val ArtworkSizeForLoading = 720.dp

@Composable
internal fun NowPlayingScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = nowPlayingViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val item = state.item
    val sizePx = with(LocalDensity.current) { ArtworkSizeForLoading.roundToPx() }
    val artwork = rememberArtwork(viewModel, item?.uri, sizePx)

    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
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
                IconButton(onClick = onBack) {
                    Icon(BoomIcons.Collapse, stringResource(R.string.feature_nowplaying_collapse))
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.feature_nowplaying_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                Spacer(Modifier.size(48.dp)) // equilibra o botão da esquerda
            }

            if (item == null) {
                BoomPlaceholder(
                    title = stringResource(R.string.feature_nowplaying_empty_title),
                    message = stringResource(R.string.feature_nowplaying_message),
                )
                return@Column
            }

            Spacer(Modifier.weight(0.5f))
            BoomArtwork(
                image = artwork,
                fallbackText = item.title,
                size = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.weight(0.5f))

            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(
                    item.artist ?: stringResource(R.string.feature_nowplaying_unknown_artist),
                    item.album,
                ).joinToString(" • "),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(24.dp))
            BoomSeekBar(
                progress = state.progress,
                isPlaying = state.isPlaying,
                onSeek = viewModel::seekToFraction,
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
            Spacer(Modifier.height(32.dp))
        }
    }
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
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onShuffle) {
            Icon(
                BoomIcons.Shuffle,
                stringResource(R.string.feature_nowplaying_shuffle),
                tint = if (shuffle) activeColor else inactiveColor,
            )
        }
        FilledTonalIconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
            Icon(BoomIcons.SkipPrevious, stringResource(R.string.feature_nowplaying_previous))
        }
        FilledIconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(80.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = IconButtonDefaults.filledIconButtonColors(),
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
        IconButton(onClick = onRepeat) {
            Icon(
                if (repeatMode == RepeatMode.One) BoomIcons.RepeatOne else BoomIcons.Repeat,
                stringResource(R.string.feature_nowplaying_repeat),
                tint = if (repeatMode == RepeatMode.Off) inactiveColor else activeColor,
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
