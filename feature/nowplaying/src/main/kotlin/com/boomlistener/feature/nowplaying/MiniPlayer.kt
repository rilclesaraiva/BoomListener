// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.core.designsystem.component.BoomArtwork
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.designsystem.component.BoomThinProgress

/**
 * Mini player que fica acima da barra de navegação. Só aparece quando há
 * uma música na fila. Tocar nele abre a tela cheia.
 */
@Composable
fun MiniPlayer(
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = nowPlayingViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val item = state.item
    val sizePx = with(LocalDensity.current) { 48.dp.roundToPx() }
    val artwork = rememberArtwork(viewModel, item?.uri, sizePx)

    AnimatedVisibility(
        visible = item != null,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clickable(onClick = onExpand),
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
