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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.core.designsystem.component.BoomArtwork
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.designsystem.component.BoomModalSheet
import com.boomlistener.core.model.Track
import com.boomlistener.core.player.ExternalPlayer
import com.boomlistener.data.library.LibraryData
import kotlinx.coroutines.delay

/**
 * Player pequeno que sobe por cima de outro app quando um áudio é aberto com o
 * BoomListener ("Abrir com"): capa, título, barra de progresso, tocar/pausar e
 * "Abrir no BoomListener". Usa o [player] próprio do pop-up: não mexe na fila
 * nem na música do player do app. Só aparece depois que a [track] foi lida.
 */
@Composable
fun ExternalPlayerPopup(
    track: Track?,
    player: ExternalPlayer,
    onOpenApp: () -> Unit,
    onClose: () -> Unit,
) {
    if (track == null) return
    val state by player.state.collectAsStateWithLifecycle()
    LaunchedEffect(player) {
        while (true) {
            delay(500)
            player.refresh()
        }
    }
    val context = LocalContext.current.applicationContext
    val sizePx = with(LocalDensity.current) { 96.dp.roundToPx() }
    val artwork by produceState<ImageBitmap?>(null, track.uri) {
        value = LibraryData.repository(context).loadArtwork(track.uri, sizePx)?.asImageBitmap()
    }
    // Arrastar mexe só na barra; busca ao soltar.
    var dragging by remember { mutableFloatStateOf(-1f) }
    val shown = if (dragging >= 0f) dragging else state.progress

    BoomModalSheet(onDismissRequest = onClose) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BoomArtwork(image = artwork, fallbackText = track.title, size = 96.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(track.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    track.artist?.let {
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
                    player.seekToFraction(dragging)
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
                FilledIconButton(onClick = player::togglePlayPause) {
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
