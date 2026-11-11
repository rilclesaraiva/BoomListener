// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.core.designsystem.component.BoomArtwork
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.designsystem.component.BoomModalSheet

/**
 * Player pequeno que sobe por cima de outro app quando um áudio é aberto com o
 * BoomListener ("Abrir com"): capa, título, barra de progresso, tocar/pausar e
 * "Abrir no BoomListener". Só aparece quando a música tocando é a de [trackUri]
 * (antes disso o player ainda mostraria a música anterior).
 */
@Composable
fun ExternalPlayerPopup(
    trackUri: String?,
    onOpenApp: () -> Unit,
    onClose: () -> Unit,
) {
    val viewModel = nowPlayingViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val item = state.item?.takeIf { it.uri == trackUri } ?: return
    val sizePx = with(LocalDensity.current) { 96.dp.roundToPx() }
    val artwork = rememberArtwork(viewModel, item.uri, sizePx)
    // Arrastar mexe só na barra; busca ao soltar.
    var dragging by remember { mutableFloatStateOf(-1f) }
    val shown = if (dragging >= 0f) dragging else state.progress

    BoomModalSheet(onDismissRequest = onClose) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BoomArtwork(image = artwork, fallbackText = item.title, size = 96.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    item.artist?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Slider(
                value = shown,
                onValueChange = { dragging = it },
                onValueChangeFinished = {
                    viewModel.seekToFraction(dragging)
                    dragging = -1f
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime((shown * state.durationMs).toLong()), style = MaterialTheme.typography.labelMedium)
                Text(formatTime(state.durationMs), style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onOpenApp) { Text(stringResource(R.string.feature_nowplaying_open_app)) }
                FilledIconButton(onClick = viewModel::togglePlayPause) {
                    if (state.isPlaying) {
                        Icon(BoomIcons.Pause, stringResource(R.string.feature_nowplaying_pause))
                    } else {
                        Icon(BoomIcons.Play, stringResource(R.string.feature_nowplaying_play))
                    }
                }
            }
        }
    }
}
