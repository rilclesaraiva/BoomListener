// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.boomlistener.core.designsystem.component.BoomLoading
import com.boomlistener.core.designsystem.theme.boomEffectsSpec
import com.boomlistener.core.designsystem.theme.boomSpatialSpec
import com.boomlistener.core.model.Lyrics
import com.boomlistener.data.lyrics.currentLyricIndex

// Depois que o usuário rola a letra com o dedo, a rolagem automática espera
// um pouco antes de voltar a acompanhar a música.
private const val AUTO_SCROLL_PAUSE_MS = 4_000L

/**
 * A letra no lugar da capa. Sincronizada: a linha atual fica grande e forte,
 * as outras menores e esmaecidas, a lista acompanha a música e tocar numa
 * linha pula para aquele trecho.
 */
@Composable
internal fun LyricsView(
    state: LyricsUiState,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        LyricsUiState.Idle, LyricsUiState.Loading -> BoomLoading(modifier)
        LyricsUiState.NotFound -> LyricsMessage(stringResource(R.string.feature_nowplaying_lyrics_not_found), modifier)
        LyricsUiState.Failed -> LyricsMessage(
            text = stringResource(R.string.feature_nowplaying_lyrics_failed),
            modifier = modifier,
            action = {
                TextButton(onClick = onRetry) { Text(stringResource(R.string.feature_nowplaying_lyrics_retry)) }
            },
        )
        is LyricsUiState.Ready -> LyricsList(state.lyrics, positionMs, onSeek, modifier)
    }
}

@Composable
private fun LyricsList(
    lyrics: Lyrics,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier,
) {
    val listState = rememberLazyListState()
    val current = if (lyrics.synced) currentLyricIndex(lyrics.lines, positionMs) else -1
    var lastUserScroll by remember { mutableLongStateOf(0L) }
    val userScrollDetector = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) lastUserScroll = System.currentTimeMillis()
                return Offset.Zero
            }
        }
    }

    // Mantém a linha atual a um terço da altura, como nos players de letra.
    LaunchedEffect(current) {
        if (current < 0) return@LaunchedEffect
        if (System.currentTimeMillis() - lastUserScroll < AUTO_SCROLL_PAUSE_MS) return@LaunchedEffect
        val viewport = listState.layoutInfo.viewportSize.height
        listState.animateScrollToItem(current, scrollOffset = -viewport / 3)
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize().nestedScroll(userScrollDetector),
    ) {
        itemsIndexed(lyrics.lines) { index, line ->
            val isCurrent = index == current
            val isPast = lyrics.synced && index < current
            LyricLineText(
                text = line.text.ifEmpty { "♪" },
                highlighted = isCurrent || !lyrics.synced,
                past = isPast,
                onClick = if (lyrics.synced) ({ onSeek(line.timeMs) }) else null,
            )
        }
        item {
            Text(
                text = stringResource(R.string.feature_nowplaying_lyrics_source, lyrics.source),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun LyricLineText(
    text: String,
    highlighted: Boolean,
    past: Boolean,
    onClick: (() -> Unit)?,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val color by animateColorAsState(
        targetValue = when {
            highlighted -> onSurface
            past -> onSurface.copy(alpha = 0.35f)
            else -> onSurface.copy(alpha = 0.5f)
        },
        animationSpec = boomEffectsSpec(),
        label = "lyricColor",
    )
    val scale by animateFloatAsState(
        targetValue = if (highlighted) 1f else 0.86f,
        animationSpec = boomSpatialSpec(),
        label = "lyricScale",
    )
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0.5f)
            }
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    )
}

@Composable
private fun LyricsMessage(
    text: String,
    modifier: Modifier,
    action: @Composable () -> Unit = {},
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            action()
        }
    }
}
