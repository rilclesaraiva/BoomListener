// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
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
    val playheadMs = sharePreview(item.uri, chosenClip, viewModel)

    // A força do som do trecho escolhido (as barrinhas saem dela).
    val levels by produceState<ClipLevelsResult?>(initialValue = null, item.uri, chosenClip) {
        // Enquanto lê o novo trecho, o desenho anterior continua (sem piscar).
        value = ClipLevelsResult(chosenClip, loadClipLevels(context, item.uri, chosenClip))
    }
    val bars = remember(levels) { levels?.sound?.levels?.let(::waveformBars) }
    val card by produceState<Bitmap?>(initialValue = null, loaded, colors, text, bars) {
        if (loaded != null) value = withContext(Dispatchers.Default) { renderShareCard(cover, text, colors, bars).bitmap }
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
                } else {
                    Surface(color = scheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth().height(PREVIEW_HEIGHT)) {
                        BoomLoading()
                    }
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
            Text(
                stringResource(R.string.feature_nowplaying_share_card_clip, formatTime(clip.startMs), formatTime(clip.endMs)),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
            )
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
                            making = scope.launch {
                                runCatching { saveShareImage(context, image) }
                                    .onSuccess { context.shareFile(it, "image/png", chooser) }
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
                            making = scope.launch {
                                try {
                                    // As barrinhas do vídeo pulsam com o som: o banner vai sem elas
                                    // e elas são desenhadas a cada quadro.
                                    val clipSound = levels?.takeIf { it.clip == clip }?.sound
                                        ?: loadClipLevels(context, item.uri, clip)
                                    val base = withContext(Dispatchers.Default) {
                                        renderShareCard(cover, text, colors, clipSound?.levels?.let(::waveformBars), drawBars = clipSound == null)
                                    }
                                    val video = exportShareVideo(context, base, clipSound?.spectrum, colors.accent, item.uri, clip)
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
@Composable
private fun sharePreview(uri: String, clip: ShareClip, viewModel: NowPlayingViewModel): Long {
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
    return position
}

private const val POSITION_TICK_MS = 50L
