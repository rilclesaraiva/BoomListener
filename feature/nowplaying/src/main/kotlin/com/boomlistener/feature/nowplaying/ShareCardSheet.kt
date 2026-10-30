// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.core.designsystem.component.BoomLoading
import com.boomlistener.core.designsystem.component.BoomModalSheet
import com.boomlistener.core.designsystem.component.rememberArtworkColor
import com.boomlistener.core.player.NowPlayingItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.boomlistener.core.designsystem.component.BoomClipSelector

/*
 * Painel "Compartilhar banner": prévia do banner, três jeitos de cor, o
 * trecho de 30 s (arrastando, a música pula para lá) e os botões de mandar
 * só a imagem ou o vídeo com o trecho.
 */

private enum class CardStyle { Cover, Theme, Dark }

/** O que vai no meio do banner: a capa grande ou a letra rolando. */
private enum class CardContent { Cover, Lyrics }

/** A capa já procurada (pode não ter capa nenhuma). */
private class LoadedCover(val bitmap: Bitmap?)

/** A força do som lida para um trecho (nula se o arquivo não pôde ser lido). */
private class ClipLevelsResult(val clip: ShareClip, val sound: ClipSound?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShareCardSheet(viewModel: NowPlayingViewModel, item: NowPlayingItem, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playback by viewModel.state.collectAsStateWithLifecycle()
    val durationMs = playback.durationMs

    // Capa grande (a do banner) e a cor que sai dela.
    val loaded by produceState<LoadedCover?>(initialValue = null, item.uri) {
        value = LoadedCover(viewModel.artwork(item.uri, COVER_PX))
    }
    val cover = loaded?.bitmap
    val seed = rememberArtworkColor(remember(cover) { cover?.asImageBitmap() })

    var style by rememberSaveable { mutableStateOf(CardStyle.Cover) }
    var content by rememberSaveable { mutableStateOf(CardContent.Cover) }
    var visualizer by rememberSaveable { mutableStateOf(ShareVisualizer.Bars) }

    // Banner com letra: a letra só é buscada quando a pessoa escolhe "Letra".
    DisposableEffect(content) {
        viewModel.setShareLyrics(content == CardContent.Lyrics)
        onDispose { viewModel.setShareLyrics(false) }
    }
    val lyricsState by viewModel.lyrics.collectAsStateWithLifecycle()
    // Só a letra sincronizada rola com a música; sem ela o banner fica com a capa.
    val syncedLyrics = (lyricsState as? LyricsUiState.Ready)
        ?.takeIf { it.uri == item.uri && it.lyrics.synced && it.lyrics.lines.isNotEmpty() }
        ?.lyrics
    val previewLyrics = remember(syncedLyrics) { syncedLyrics?.let { ShareLyricsLayout(it.lines, SHARE_LYRICS_WIDTH) } }
    val showLyrics = content == CardContent.Lyrics && previewLyrics != null
    val scheme = MaterialTheme.colorScheme
    val seedArgb = (seed ?: scheme.primary).toArgb()
    val colorsFor: (CardStyle) -> ShareCardColors = { option ->
        when (option) {
            CardStyle.Cover -> coverCardColors(seedArgb)
            CardStyle.Theme -> ShareCardColors(
                background = scheme.primaryContainer.toArgb(),
                content = scheme.onPrimaryContainer.toArgb(),
                accent = scheme.primary.toArgb(),
            )
            CardStyle.Dark -> darkCardColors(seedArgb)
        }
    }
    val colors = colorsFor(style)
    val text = ShareCardText(
        label = stringResource(R.string.feature_nowplaying_share_card_label),
        title = item.title,
        artist = item.artist ?: stringResource(R.string.feature_nowplaying_unknown_artist),
        appName = SHARE_CARD_APP_NAME,
    )
    // O trecho começa sempre no começo da música; a pessoa arrasta para escolher.
    var startMs by remember(item.uri) { mutableLongStateOf(0L) }
    val clip = ShareClip.at(startMs, durationMs)
    // Trecho "assentado" (depois de soltar o dedo): é o que toca e o que dá o desenho.
    var chosenStart by remember(item.uri) { mutableLongStateOf(0L) }
    val chosenClip = ShareClip.at(chosenStart, durationMs)
    val preview = sharePreview(item.uri, chosenClip, viewModel)
    val playheadMs = preview.positionMs

    // A força do som do trecho escolhido (as barrinhas saem dela).
    val levels by produceState<ClipLevelsResult?>(initialValue = null, item.uri, chosenClip) {
        // Enquanto lê o novo trecho, o desenho anterior continua (sem piscar).
        value = ClipLevelsResult(chosenClip, loadClipLevels(context, item.uri, chosenClip))
    }
    val bars = remember(levels) { levels?.sound?.levels?.let(::waveformBars) }
    // Com letra, a prévia vem sem ela: a letra rola por cima, junto com o trecho.
    val card by produceState<Bitmap?>(initialValue = null, loaded, colors, text, bars, showLyrics, previewLyrics, visualizer) {
        if (loaded != null) {
            value = withContext(Dispatchers.Default) {
                renderShareCard(cover, text, colors, bars, visualizer = visualizer, lyrics = previewLyrics?.takeIf { showLyrics }).bitmap
            }
        }
    }
    val maxStart = ShareClip.maxStart(durationMs)

    var making by remember { mutableStateOf<Job?>(null) }
    var failed by remember { mutableStateOf(false) }
    val chooser = stringResource(R.string.feature_nowplaying_share_card_chooser)

    BoomModalSheet(onDismissRequest = onDismiss) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                stringResource(R.string.feature_nowplaying_share_card),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(PREVIEW_HEIGHT)
                    .aspectRatio(SHARE_CARD_WIDTH.toFloat() / SHARE_CARD_HEIGHT)
                    .clip(MaterialTheme.shapes.large),
            ) {
                val image = card
                if (image != null) {
                    Image(
                        bitmap = remember(image) { image.asImageBitmap() },
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                    )
                    if (showLyrics && previewLyrics != null) {
                        PreviewLyrics(
                            layout = previewLyrics,
                            clipStartMs = chosenClip.startMs,
                            livePositionMs = preview.livePositionMs,
                            color = colors.content,
                            modifier = Modifier.matchParentSize(),
                        )
                    }
                } else {
                    Surface(color = scheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth().height(PREVIEW_HEIGHT)) {
                        BoomLoading()
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            // Capa grande ou a letra rolando no meio do banner.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CardContent.entries.forEach { option ->
                    FilterChip(
                        selected = content == option,
                        onClick = { content = option },
                        label = {
                            Text(
                                stringResource(
                                    when (option) {
                                        CardContent.Cover -> R.string.feature_nowplaying_share_card_content_cover
                                        CardContent.Lyrics -> R.string.feature_nowplaying_share_card_content_lyrics
                                    },
                                ),
                            )
                        },
                        enabled = making == null,
                    )
                }
            }
            if (content == CardContent.Lyrics && previewLyrics == null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(
                        when (val state = lyricsState) {
                            LyricsUiState.Idle, LyricsUiState.Loading -> R.string.feature_nowplaying_share_card_lyrics_loading
                            is LyricsUiState.Ready ->
                                if (state.uri == item.uri) R.string.feature_nowplaying_share_card_lyrics_unsynced
                                else R.string.feature_nowplaying_share_card_lyrics_loading
                            LyricsUiState.NotFound, LyricsUiState.Failed -> R.string.feature_nowplaying_share_card_lyrics_missing
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(12.dp))
            // O desenho do som no banner.
            Text(
                stringResource(R.string.feature_nowplaying_share_card_visualizer),
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShareVisualizer.entries.forEach { option ->
                    FilterChip(
                        selected = visualizer == option,
                        onClick = { visualizer = option },
                        label = {
                            Text(
                                stringResource(
                                    when (option) {
                                        ShareVisualizer.Bars -> R.string.feature_nowplaying_share_card_visualizer_bars
                                        ShareVisualizer.Wave -> R.string.feature_nowplaying_share_card_visualizer_wave
                                        ShareVisualizer.None -> R.string.feature_nowplaying_share_card_visualizer_none
                                    },
                                ),
                            )
                        },
                        enabled = making == null,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CardStyle.entries.forEach { option ->
                    val name = stringResource(
                        when (option) {
                            CardStyle.Cover -> R.string.feature_nowplaying_share_card_color_cover
                            CardStyle.Theme -> R.string.feature_nowplaying_share_card_color_theme
                            CardStyle.Dark -> R.string.feature_nowplaying_share_card_color_dark
                        },
                    )
                    Surface(
                        selected = style == option,
                        onClick = { style = option },
                        shape = CircleShape,
                        color = Color(colorsFor(option).background),
                        border = BorderStroke(
                            if (style == option) 3.dp else 1.dp,
                            if (style == option) scheme.primary else scheme.outlineVariant,
                        ),
                        modifier = Modifier.size(SWATCH_SIZE).semantics { contentDescription = name },
                    ) {}
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.feature_nowplaying_share_card_clip, formatTime(clip.startMs), formatTime(clip.endMs)),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                // O ponto exato que a prévia está tocando (só com o trecho assentado).
                if (clip == chosenClip) {
                    Text(
                        stringResource(R.string.feature_nowplaying_share_card_playing_at, formatTime(chosenClip.startMs + playheadMs)),
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.primary,
                    )
                }
            }
            if (maxStart > 0) {
                Spacer(Modifier.height(8.dp))
                BoomClipSelector(
                    totalMs = durationMs,
                    startMs = clip.startMs,
                    lengthMs = clip.lengthMs,
                    // O ponto que está tocando só aparece com o trecho assentado.
                    playheadMs = playheadMs.takeIf { clip == chosenClip },
                    onStartChange = { startMs = it },
                    // Solto o dedo, a prévia passa a tocar o trecho escolhido.
                    onChangeFinished = { chosenStart = clip.startMs },
                    enabled = making == null,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.feature_nowplaying_share_card_clip_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (failed) {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.feature_nowplaying_share_card_error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.error,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(20.dp))
            if (making != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(BUTTON_ROW_HEIGHT)) {
                    Box(Modifier.size(BUTTON_ROW_HEIGHT)) { BoomLoading() }
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.feature_nowplaying_share_card_making), style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(
                        onClick = {
                            val image = card ?: return@FilledTonalButton
                            val lyricsLines = syncedLyrics?.lines?.takeIf { showLyrics }
                            making = scope.launch {
                                runCatching {
                                    // Com letra, a imagem mostra o primeiro verso do trecho em destaque.
                                    val still = if (lyricsLines != null) {
                                        withContext(Dispatchers.Default) {
                                            val layout = ShareLyricsLayout(lyricsLines, SHARE_LYRICS_WIDTH)
                                            renderShareCard(
                                                cover, text, colors, bars,
                                                visualizer = visualizer,
                                                lyrics = layout,
                                                lyricsTimeMs = staticLyricsTime(layout.times, clip),
                                            ).bitmap
                                        }
                                    } else {
                                        image
                                    }
                                    saveShareImage(context, still)
                                }.onSuccess { context.shareFile(it, "image/png", chooser) }
                                making = null
                            }
                        },
                        enabled = card != null,
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.feature_nowplaying_share_card_image)) }
                    Button(
                        onClick = {
                            if (card == null) return@Button
                            failed = false
                            val lyricsLines = syncedLyrics?.lines?.takeIf { showLyrics }
                            making = scope.launch {
                                try {
                                    // As barrinhas do vídeo pulsam com o som: o banner vai sem elas
                                    // e elas são desenhadas a cada quadro.
                                    val clipSound = levels?.takeIf { it.clip == clip }?.sound
                                        ?: loadClipLevels(context, item.uri, clip)
                                    // A letra do vídeo é desenhada à parte da prévia (outra thread).
                                    val videoLyrics = lyricsLines?.let { ShareLyricsLayout(it, SHARE_LYRICS_WIDTH) }
                                    val base = withContext(Dispatchers.Default) {
                                        renderShareCard(
                                            cover, text, colors, clipSound?.levels?.let(::waveformBars),
                                            drawBars = clipSound == null,
                                            visualizer = visualizer,
                                            lyrics = videoLyrics,
                                        )
                                    }
                                    val video = exportShareVideo(
                                        context, base, clipSound?.spectrum, colors.accent, item.uri, clip,
                                        lyrics = videoLyrics,
                                        lyricsColor = colors.content,
                                        visualizer = visualizer,
                                    )
                                    context.shareFile(video, "video/mp4", chooser)
                                } catch (cancel: CancellationException) {
                                    throw cancel
                                } catch (error: Exception) {
                                    failed = true
                                } finally {
                                    making = null
                                }
                            }
                        },
                        enabled = card != null,
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.feature_nowplaying_share_card_video)) }
                }
            }
        }
    }
}

private const val COVER_PX = 780
private val PREVIEW_HEIGHT = 360.dp
private val SWATCH_SIZE = 44.dp
private val BUTTON_ROW_HEIGHT = 48.dp

/**
 * Toca só o trecho escolhido, em volta, enquanto o painel está aberto, e
 * devolve o ponto que está tocando dentro dele. A música que estava tocando
 * pausa ao abrir e volta ao fechar; a prévia é um player à parte (não mexe na
 * fila nem no ponto da música) e pausa quando o app sai da tela.
 */
/** O ponto da prévia: [positionMs] algumas vezes por segundo e [livePositionMs] na hora (para animar a cada quadro). */
private class SharePreviewState(val positionMs: Long, val livePositionMs: () -> Long)

@Composable
private fun sharePreview(uri: String, clip: ShareClip, viewModel: NowPlayingViewModel): SharePreviewState {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val player = remember {
        ExoPlayer.Builder(context.applicationContext)
            // Sem pedir o foco de áudio: o player principal já está pausado por nós
            // e, se perdesse o foco, não voltaria a tocar sozinho ao fechar.
            .setAudioAttributes(AudioAttributes.DEFAULT, false)
            .build()
            .apply { repeatMode = Player.REPEAT_MODE_ONE }
    }
    DisposableEffect(player, lifecycle) {
        val wasPlaying = viewModel.state.value.isPlaying
        if (wasPlaying) viewModel.togglePlayPause()
        // App fora da tela: a prévia para (não toca em segundo plano); voltando, continua.
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> player.pause()
                Lifecycle.Event.ON_START -> player.play()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            player.release()
            if (wasPlaying && !viewModel.state.value.isPlaying) viewModel.togglePlayPause()
        }
    }
    LaunchedEffect(player, uri, clip) {
        player.setMediaItem(
            MediaItem.Builder()
                .setUri(uri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(clip.startMs)
                        .setEndPositionMs(clip.endMs)
                        .build(),
                )
                .build(),
        )
        player.prepare()
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) player.play()
    }
    // O ponto que está tocando, lido algumas vezes por segundo.
    val position by produceState(initialValue = 0L, player, clip) {
        while (true) {
            value = player.currentPosition.coerceIn(0, clip.lengthMs)
            delay(POSITION_TICK_MS)
        }
    }
    return SharePreviewState(position) { player.currentPosition.coerceIn(0, clip.lengthMs) }
}

/**
 * A letra rolando por cima da prévia, a cada quadro, no ponto que a prévia
 * está tocando: o mesmo desenho do vídeo, na escala da prévia.
 */
@Composable
private fun PreviewLyrics(
    layout: ShareLyricsLayout,
    clipStartMs: Long,
    livePositionMs: () -> Long,
    color: Int,
    modifier: Modifier = Modifier,
) {
    val latestPosition by rememberUpdatedState(livePositionMs)
    var timeMs by remember { mutableLongStateOf(clipStartMs) }
    LaunchedEffect(clipStartMs) {
        // O player só atualiza o ponto aos pulinhos: entre um e outro, a letra
        // segue o relógio (o mesmo cálculo da bolinha do player), senão a
        // passagem de um verso para o outro andava em degraus.
        var shown = latestPosition().toFloat()
        var reported = latestPosition()
        var reportedAt = System.nanoTime()
        var last = reportedAt
        while (true) {
            withFrameNanos {
                val now = System.nanoTime()
                val position = latestPosition()
                if (position != reported) {
                    reported = position
                    reportedAt = now
                }
                shown = nextSmoothPositionMs(
                    shownMs = shown,
                    reportedMs = reported,
                    sinceReportMs = (now - reportedAt) / 1_000_000f,
                    frameMs = (now - last) / 1_000_000f,
                    speed = 1f,
                    durationMs = ShareClip.MAX_MS,
                )
                last = now
                timeMs = clipStartMs + shown.toLong()
            }
        }
    }
    Canvas(modifier) {
        val scale = size.width / SHARE_CARD_WIDTH
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            native.save()
            native.scale(scale, scale)
            layout.draw(native, SHARE_LYRICS_AREA, shareLyricsFrame(layout.times, timeMs), color)
            native.restore()
        }
    }
}

private const val POSITION_TICK_MS = 50L
