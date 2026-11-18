// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.profile

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.rememberCoroutineScope
import android.graphics.Picture
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.core.designsystem.component.BoomArtwork
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.designsystem.component.BoomImageCycle
import com.boomlistener.core.designsystem.component.BoomLoading
import com.boomlistener.core.designsystem.component.LocalBottomOverlay
import com.boomlistener.core.designsystem.component.rememberImageSet
import com.boomlistener.core.designsystem.theme.WrappedColors
import com.boomlistener.core.designsystem.theme.boomEffectsSpec
import com.boomlistener.data.wellbeing.StatsPeriod
import com.boomlistener.data.wellbeing.ArtistRank
import com.boomlistener.data.wellbeing.StatsReport
import com.boomlistener.data.wellbeing.smoothHourLevels
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/** O destaque do cartão que está sendo desenhado (títulos e números grandes). */
private val LocalWrappedAccent = androidx.compose.runtime.compositionLocalOf { Color.Unspecified }

/** O fundo do cartão que está sendo desenhado: o texto que fica em cima do destaque usa esta cor. */
private val LocalWrappedBackdrop = androidx.compose.runtime.compositionLocalOf { Color.Unspecified }

/** Tempo de cada cartão antes de passar sozinho. */
private const val PAGE_MS = 7_000f
private const val TAP_BACK_SHARE = 0.35f
private const val MAX_GENRES = 5
private const val MAX_TOP = 5

private enum class WrappedCard { Intro, Artists, Song, Genres, Time, Hour, Discoveries, Sprint, Top, Summary }

/**
 * O Wrapped: cartões em sequência (toque à direita avança, à esquerda volta, segurar
 * pausa) com o que mais marcou o período escolhido nas Estatísticas. A música mais
 * ouvida toca aqui dentro (o player principal pausa e volta ao sair). Tudo calculado
 * no aparelho.
 */
@Composable
internal fun WrappedScreen(
    viewModel: MyProfileViewModel,
    period: StatsPeriod,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(period) { viewModel.selectPeriod(period) }
    val report by viewModel.report.collectAsStateWithLifecycle()
    val song by viewModel.wrappedSong.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val area = remember(viewModel) { viewModel.areaPlayer(context) }
    DisposableEffect(area, lifecycle) {
        area.open()
        // App fora da tela: a música da área para (não toca em segundo plano); voltando, continua.
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> area.pause()
                Lifecycle.Event.ON_START -> area.resume()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            area.close()
        }
    }
    val songUri = song?.uri
    LaunchedEffect(area, songUri) { songUri?.let(area::play) }
    BackHandler(onBack = onBack)
    // Os cartões são sempre escuros: ícones claros na barra de cima enquanto o Wrapped está aberto.
    val view = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(view) {
        var host = view.context
        while (host is android.content.ContextWrapper && host !is android.app.Activity) host = host.baseContext
        val window = (host as? android.app.Activity)?.window
        val controller = window?.let { androidx.core.view.WindowCompat.getInsetsController(it, view) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose { if (controller != null && previous != null) controller.isAppearanceLightStatusBars = previous }
    }

    val current = report?.takeIf { it.period == period }
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        if (current == null) {
            BoomLoading(Modifier.fillMaxSize())
        } else {
            WrappedCards(current, viewModel, hasSong = song != null, onMutedChange = area::setMuted, onBack = onBack)
        }
    }
}

@Composable
private fun WrappedCards(report: StatsReport, viewModel: MyProfileViewModel, hasSong: Boolean, onMutedChange: (Boolean) -> Unit, onBack: () -> Unit) {
    val cards = remember(report, hasSong) {
        buildList {
            add(WrappedCard.Intro)
            if (report.artists.isNotEmpty()) add(WrappedCard.Artists)
            if (hasSong) add(WrappedCard.Song)
            if (report.genres.isNotEmpty()) add(WrappedCard.Genres)
            add(WrappedCard.Time)
            if (report.favoriteHour != null) add(WrappedCard.Hour)
            if (report.discoveryCount > 0) add(WrappedCard.Discoveries)
            if (report.sprint != null) add(WrappedCard.Sprint)
            if (report.songs.isNotEmpty()) add(WrappedCard.Top)
            // O resumo fecha o Wrapped: o melhor de tudo numa tela só, para compartilhar.
            if (report.songs.isNotEmpty() || report.artists.isNotEmpty()) add(WrappedCard.Summary)
        }
    }
    var page by remember(cards) { mutableIntStateOf(0) }
    var paused by remember { mutableStateOf(false) }
    // Segurando o dedo na tela: barras e botões se esvaem; ao soltar, voltam com a mesma animação.
    var holding by remember { mutableStateOf(false) }
    // Som da música da área: o botão do topo silencia só ela (o player principal já está pausado).
    var muted by remember { mutableStateOf(false) }
    LaunchedEffect(muted) { onMutedChange(muted) }
    val progress = remember(page, cards) { mutableFloatStateOf(0f) }

    // Passa sozinho a cada PAGE_MS (pelo relógio dos quadros: não é um movimento do tema); segurar pausa.
    LaunchedEffect(page, paused, cards) {
        if (page >= cards.lastIndex) {
            progress.floatValue = 1f
            return@LaunchedEffect
        }
        var last = androidx.compose.runtime.withFrameNanos { it }
        while (progress.floatValue < 1f) {
            val now = androidx.compose.runtime.withFrameNanos { it }
            if (!paused) progress.floatValue = (progress.floatValue + (now - last) / 1_000_000f / PAGE_MS).coerceAtMost(1f)
            last = now
        }
        page += 1
    }

    val barColors = WrappedColors.cards[page.coerceAtLeast(0) % WrappedColors.cards.size]
    val effects = boomEffectsSpec<Float>()
    val capture = remember { WrappedCapture() }
    var sharing by remember { mutableStateOf(false) }
    val summaryIndex = cards.indexOf(WrappedCard.Summary)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val chooser = stringResource(R.string.feature_profile_wrapped_share)
    // Compartilhar manda sempre o resumo (a última tela), de qualquer cartão: ele é desenhado
    // escondido, por trás de tudo, e o desenho dele é o que vira a imagem.
    val share: () -> Unit = {
        if (capture.picture != null && !sharing) {
            scope.launch {
                sharing = true
                val image = capture.picture?.toBitmap()
                sharing = false
                if (image != null) runCatching { context.shareWrappedCard(image, chooser) }
            }
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(cards) {
                detectTapGestures(
                    onPress = {
                        paused = true
                        tryAwaitRelease()
                        paused = false
                        holding = false
                    },
                    // Segurou: pausa e esvai as barras; soltar depois disso NÃO vale como toque (não troca de cartão).
                    onLongPress = { holding = true },
                    onTap = { offset ->
                        if (offset.x < size.width * TAP_BACK_SHARE) page = (page - 1).coerceAtLeast(0)
                        else page = (page + 1).coerceAtMost(cards.lastIndex)
                    },
                )
            },
    ) {
        if (summaryIndex >= 0) {
            val summaryPalette = WrappedColors.cards[summaryIndex % WrappedColors.cards.size]
            Surface(
                color = summaryPalette.container,
                contentColor = summaryPalette.onContainer,
                modifier = Modifier.fillMaxSize().drawWithContent {
                    val picture = Picture()
                    val canvas = Canvas(picture.beginRecording(size.width.toInt(), size.height.toInt()))
                    draw(this, layoutDirection, canvas, size) { this@drawWithContent.drawContent() }
                    picture.endRecording()
                    // Só guarda o desenho: nada aparece aqui (o resumo de verdade é o cartão da frente).
                    capture.picture = picture
                },
            ) {
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalWrappedAccent provides summaryPalette.accent,
                    LocalWrappedBackdrop provides summaryPalette.container,
                ) {
                    CardColumn {
                        SummaryCard(report, viewModel, showActions = false, onShare = {})
                        Text(
                            stringResource(R.string.feature_profile_wrapped_brand),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    }
                }
            }
        }
        AnimatedContent(
            targetState = page,
            transitionSpec = { fadeIn(effects) togetherWith fadeOut(effects) },
            label = "wrapped-card",
            modifier = Modifier.fillMaxSize(),
        ) { index ->
            val card = cards.getOrNull(index) ?: WrappedCard.Intro
            val palette = WrappedColors.cards[index % WrappedColors.cards.size]
            Surface(color = palette.container, contentColor = palette.onContainer, modifier = Modifier.fillMaxSize()) {
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalWrappedAccent provides palette.accent,
                    LocalWrappedBackdrop provides palette.container,
                ) {
                    CardColumn {
                        when (card) {
                            WrappedCard.Intro -> IntroCard(report, viewModel)
                            WrappedCard.Artists -> ArtistsCard(report, viewModel)
                            WrappedCard.Song -> SongCard(report, viewModel)
                            WrappedCard.Genres -> GenresCard(report)
                            WrappedCard.Time -> TimeCard(report)
                            WrappedCard.Hour -> HourCard(report)
                            WrappedCard.Discoveries -> DiscoveriesCard(report, viewModel)
                            WrappedCard.Sprint -> SprintWrappedCard(report, viewModel)
                            WrappedCard.Top -> TopCard(report, viewModel, showActions = true)
                            WrappedCard.Summary -> SummaryCard(report, viewModel, showActions = true, onShare = share)
                        }
                    }
                }
            }
        }
        // Barras de progresso e fechar, por cima de tudo.
        val chromeAlpha by animateFloatAsState(if (holding) 0f else 1f, effects, label = "wrapped-chrome")
        Column(Modifier.fillMaxWidth().alpha(chromeAlpha).statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                cards.indices.forEach { index ->
                    val value by remember(index, page) {
                        derivedStateOf { if (index < page) 1f else if (index == page) progress.floatValue else 0f }
                    }
                    LinearProgressIndicator(
                        progress = { value },
                        modifier = Modifier.weight(1f).height(4.dp),
                        color = barColors.onContainer,
                        trackColor = barColors.onContainer.copy(alpha = 0.3f),
                        drawStopIndicator = {},
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (hasSong) {
                    IconButton(onClick = { muted = !muted }) {
                        Icon(
                            if (muted) BoomIcons.VolumeOff else BoomIcons.VolumeUp,
                            contentDescription = stringResource(
                                if (muted) R.string.feature_profile_wrapped_unmute else R.string.feature_profile_wrapped_mute,
                            ),
                        )
                    }
                }
                if (summaryIndex >= 0) IconButton(
                    onClick = share,
                ) {
                    Icon(BoomIcons.Share, contentDescription = stringResource(R.string.feature_profile_wrapped_share))
                }
                IconButton(onClick = onBack) {
                    Icon(BoomIcons.Close, contentDescription = stringResource(R.string.feature_profile_wrapped_close))
                }
            }
        }
    }
}

/** A coluna de conteúdo de um cartão, com as margens de tela inteira. */
@Composable
private fun CardColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(top = 56.dp, bottom = LocalBottomOverlay.current + 24.dp)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

@Composable
private fun CardTitle(text: String) {
    Text(text, style = MaterialTheme.typography.headlineMedium, maxLines = 3, overflow = TextOverflow.Ellipsis, color = LocalWrappedAccent.current)
}

@Composable
private fun periodLabel(report: StatsReport): String = when (report.period) {
    StatsPeriod.Last4Weeks -> stringResource(R.string.feature_profile_stats_period_4weeks)
    StatsPeriod.ThisMonth -> stringResource(R.string.feature_profile_stats_period_month)
    StatsPeriod.ThisYear -> report.to.year.toString()
    StatsPeriod.AllTime -> stringResource(R.string.feature_profile_stats_period_all)
}

/** Capa de uma música, preenchendo o espaço dado. */
@Composable
private fun Cover(viewModel: MyProfileViewModel, uri: String, title: String, modifier: Modifier = Modifier) {
    val image = rememberLocalArtwork(viewModel, uri, 160.dp)
    BoomArtwork(image = image, fallbackText = title, modifier = modifier, size = null)
}

@Composable
private fun ArtistAvatar(viewModel: MyProfileViewModel, name: String, size: androidx.compose.ui.unit.Dp) {
    val px = with(LocalDensity.current) { size.roundToPx() }
    val photos = rememberImageSet(remember(name, px) { artistAttempts(name, px, viewModel) })
    BoomImageCycle(photos) { shown -> Avatar(name, size = size, image = shown) }
}

/** Linha-cartão translúcida, no tom do cartão: o fundo das listas que enchem a tela. */
@Composable
private fun RowCard(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val ink = LocalContentColor.current
    Surface(color = ink.copy(alpha = 0.14f), contentColor = ink, shape = MaterialTheme.shapes.extraLarge, modifier = modifier) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

/** Número de posição, grande, no destaque do cartão. */
@Composable
private fun RankNumber(position: Int) {
    Text(
        position.toString(),
        style = MaterialTheme.typography.headlineLarge,
        color = LocalWrappedAccent.current,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = Modifier.width(36.dp),
    )
}

@Composable
private fun ColumnScope.IntroCard(report: StatsReport, viewModel: MyProfileViewModel) {
    Text(periodLabel(report), style = MaterialTheme.typography.titleLarge)
    Text(
        stringResource(R.string.feature_profile_wrapped_intro),
        style = MaterialTheme.typography.displayMedium,
        color = LocalWrappedAccent.current,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )
    // Capas dos álbuns mais ouvidos (ou das músicas, sem álbum), em mosaico que enche o que sobra da tela.
    val covers = remember(report) {
        report.albums.map { it.coverUri to it.album }.ifEmpty { report.songs.map { it.uri to it.title } }.take(5)
    }
    CoverMosaic(viewModel, covers, Modifier.weight(1f).fillMaxWidth())
}

@Composable
private fun CoverMosaic(viewModel: MyProfileViewModel, covers: List<Pair<String, String>>, modifier: Modifier) {
    val gap = Arrangement.spacedBy(8.dp)
    @Composable
    fun tile(index: Int, tileModifier: Modifier) = Cover(viewModel, covers[index].first, covers[index].second, tileModifier)
    when (covers.size) {
        0 -> Spacer(modifier)
        1 -> tile(0, modifier)
        2 -> Row(modifier, horizontalArrangement = gap) {
            tile(0, Modifier.weight(1f).fillMaxHeight())
            tile(1, Modifier.weight(1f).fillMaxHeight())
        }
        3 -> Row(modifier, horizontalArrangement = gap) {
            tile(0, Modifier.weight(1.2f).fillMaxHeight())
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = gap) {
                tile(1, Modifier.weight(1f).fillMaxWidth())
                tile(2, Modifier.weight(1f).fillMaxWidth())
            }
        }
        4 -> Column(modifier, verticalArrangement = gap) {
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = gap) {
                tile(0, Modifier.weight(1f).fillMaxHeight())
                tile(1, Modifier.weight(1f).fillMaxHeight())
            }
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = gap) {
                tile(2, Modifier.weight(1f).fillMaxHeight())
                tile(3, Modifier.weight(1f).fillMaxHeight())
            }
        }
        else -> Column(modifier, verticalArrangement = gap) {
            Row(Modifier.weight(1.5f).fillMaxWidth(), horizontalArrangement = gap) {
                tile(0, Modifier.weight(1.2f).fillMaxHeight())
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = gap) {
                    tile(1, Modifier.weight(1f).fillMaxWidth())
                    tile(2, Modifier.weight(1f).fillMaxWidth())
                }
            }
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = gap) {
                tile(3, Modifier.weight(1f).fillMaxHeight())
                tile(4, Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun ColumnScope.ArtistsCard(report: StatsReport, viewModel: MyProfileViewModel) {
    CardTitle(stringResource(R.string.feature_profile_wrapped_artists))
    val top = report.artists.take(MAX_TOP)
    // Pódio: o 1º no centro, grande e mais alto; o 2º à esquerda e o 3º à direita, nos degraus menores.
    Row(
        Modifier.weight(1f).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        listOf(1, 0, 2).forEach { index ->
            top.getOrNull(index)?.let { artist ->
                PodiumColumn(
                    rank = index + 1,
                    artist = artist,
                    viewModel = viewModel,
                    modifier = Modifier.weight(if (index == 0) 1.3f else 1f),
                )
            }
        }
    }
    // 4º e 5º: linhas com o ícone (foto) de cada um ao lado do nome.
    top.drop(3).forEachIndexed { index, artist ->
        RowCard(Modifier.fillMaxWidth()) {
            RankNumber(index + 4)
            ArtistAvatar(viewModel, artist.name, 40.dp)
            Column(Modifier.weight(1f)) {
                Text(artist.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    pluralStringResource(R.plurals.feature_profile_plays, artist.plays, artist.plays),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/** Uma coluna do pódio: foto, nome, quantas vezes e o degrau com a posição. */
@Composable
private fun PodiumColumn(rank: Int, artist: ArtistRank, viewModel: MyProfileViewModel, modifier: Modifier) {
    val first = rank == 1
    val step = when (rank) {
        1 -> PODIUM_STEP_FIRST
        2 -> PODIUM_STEP_SECOND
        else -> PODIUM_STEP_THIRD
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        ArtistAvatar(viewModel, artist.name, if (first) 92.dp else 64.dp)
        Spacer(Modifier.height(8.dp))
        Text(
            artist.name,
            style = if (first) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            pluralStringResource(R.plurals.feature_profile_plays, artist.plays, artist.plays),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
        if (first) Text(minutesText(artist.minutes), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        val ink = LocalContentColor.current
        Surface(
            color = if (first) LocalWrappedAccent.current else ink.copy(alpha = 0.14f),
            contentColor = if (first) LocalWrappedBackdrop.current else LocalWrappedAccent.current,
            shape = MaterialTheme.shapes.large.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp)),
            modifier = Modifier.fillMaxWidth().height(step),
        ) {
            Box(Modifier.fillMaxSize().padding(top = 8.dp), contentAlignment = Alignment.TopCenter) {
                Text(rank.toString(), style = MaterialTheme.typography.headlineLarge, maxLines = 1)
            }
        }
    }
}

private val PODIUM_STEP_FIRST = 120.dp
private val PODIUM_STEP_SECOND = 84.dp
private val PODIUM_STEP_THIRD = 56.dp

@Composable
private fun ColumnScope.SongCard(report: StatsReport, viewModel: MyProfileViewModel) {
    // A música que toca aqui: a mais ouvida, ou a seguinte se a mais ouvida saiu do aparelho.
    val playing by viewModel.wrappedSong.collectAsStateWithLifecycle()
    val song = playing ?: return
    val isTop = report.songs.firstOrNull()?.uri == song.uri
    CardTitle(
        stringResource(if (isTop) R.string.feature_profile_wrapped_song else R.string.feature_profile_wrapped_song_fallback),
    )
    // A capa enche o que sobra da tela, quadrada.
    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
        // O lado é o menor entre a largura e a altura livres: quadrada e sempre dentro das margens.
        val side = if (maxWidth < maxHeight) maxWidth else maxHeight
        Cover(viewModel, song.uri, song.title, Modifier.size(side))
    }
    Column {
        Text(song.title, style = MaterialTheme.typography.headlineMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        song.artist?.let { Text(it, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(song.plays.toString(), style = MaterialTheme.typography.displayLarge, color = LocalWrappedAccent.current, maxLines = 1)
        Column {
            Text(stringResource(R.string.feature_profile_wrapped_song_times), style = MaterialTheme.typography.titleLarge)
            Text(minutesText(song.minutes), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ColumnScope.GenresCard(report: StatsReport) {
    CardTitle(stringResource(R.string.feature_profile_wrapped_genres))
    // Mapa de blocos: cada gênero ocupa uma área proporcional ao quanto foi ouvido.
    val genres = report.genres.take(MAX_GENRES)
    if (genres.isNotEmpty()) GenreTreemap(genres, 0, horizontal = false, Modifier.weight(1f).fillMaxWidth())
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(report.genreCount.toString(), style = MaterialTheme.typography.displaySmall, color = LocalWrappedAccent.current)
        Text(
            pluralStringResource(R.plurals.feature_profile_stats_genres, report.genreCount),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 6.dp),
        )
    }
}

/** Divide o espaço entre os gêneros: o primeiro fica com a sua fatia e o resto se divide no que sobrou, alternando a direção. */
@Composable
private fun GenreTreemap(genres: List<com.boomlistener.data.wellbeing.GenreRank>, start: Int, horizontal: Boolean, modifier: Modifier) {
    val genre = genres[start]
    if (start == genres.lastIndex) {
        GenreTile(genre, start, modifier)
        return
    }
    val first = genre.share.coerceAtLeast(MIN_GENRE_SHARE)
    val rest = genres.drop(start + 1).sumOf { it.share.coerceAtLeast(MIN_GENRE_SHARE).toDouble() }.toFloat()
    if (horizontal) {
        Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GenreTile(genre, start, Modifier.weight(first).fillMaxHeight())
            GenreTreemap(genres, start + 1, false, Modifier.weight(rest).fillMaxHeight())
        }
    } else {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GenreTile(genre, start, Modifier.weight(first).fillMaxWidth())
            GenreTreemap(genres, start + 1, true, Modifier.weight(rest).fillMaxWidth())
        }
    }
}

private const val MIN_GENRE_SHARE = 0.1f

@Composable
private fun GenreTile(genre: com.boomlistener.data.wellbeing.GenreRank, index: Int, modifier: Modifier) {
    val block = WrappedColors.blocks[index % WrappedColors.blocks.size]
    Surface(color = block.fill, contentColor = block.onFill, shape = MaterialTheme.shapes.extraLarge, modifier = modifier) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.Bottom) {
            Text(
                stringResource(R.string.feature_profile_stats_percent, (genre.share * 100).toInt()),
                style = if (index == 0) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineSmall,
                maxLines = 1,
            )
            Text(genre.genre, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ColumnScope.TimeCard(report: StatsReport) {
    CardTitle(stringResource(R.string.feature_profile_wrapped_time))
    // O número de minutos, enorme, é o assunto do cartão e ocupa o que sobra acima dos destaques.
    Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center) {
        Text(
            java.text.NumberFormat.getIntegerInstance().format(report.minutes),
            style = MaterialTheme.typography.displayLarge,
            color = LocalWrappedAccent.current,
            maxLines = 1,
            softWrap = false,
        )
        Text(stringResource(R.string.feature_profile_wrapped_minutes), style = MaterialTheme.typography.headlineMedium)
        if (report.minutes >= 60) Text(minutesText(report.minutes), style = MaterialTheme.typography.titleMedium)
    }
    val blocks = WrappedColors.blocks
    report.bestDay?.let { best ->
        HighlightRow(
            blocks[0], best.date.dayOfMonth.toString(),
            stringResource(R.string.feature_profile_stats_record_day), best.date.format(DateShown), minutesText(best.minutes),
            Modifier,
        )
    }
    if (report.longestStreak > 1) {
        HighlightRow(
            blocks[1], report.longestStreak.toString(),
            stringResource(R.string.feature_profile_stats_streak),
            pluralStringResource(R.plurals.feature_profile_stats_day_count, report.longestStreak, report.longestStreak), null,
            Modifier,
        )
    }
    report.mostArtistsDay?.let { most ->
        HighlightRow(
            blocks[2], most.artists.toString(),
            stringResource(R.string.feature_profile_stats_most_artists_day),
            pluralStringResource(R.plurals.feature_profile_stats_artist_count, most.artists, most.artists), most.date.format(DateShown),
            Modifier,
        )
    }
}

/** Destaque do cartão de tempo: o número grande, na cor do bloco, e ao lado o que ele significa. */
@Composable
private fun HighlightRow(
    block: com.boomlistener.core.designsystem.theme.WrappedBlockColors,
    number: String,
    label: String,
    value: String,
    detail: String?,
    modifier: Modifier,
) {
    RowCard(modifier.fillMaxWidth()) {
        Text(
            number,
            style = MaterialTheme.typography.displayMedium,
            color = block.fill,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.width(96.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = LocalContentColor.current.copy(alpha = 0.75f))
            Text(value, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            detail?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = LocalContentColor.current.copy(alpha = 0.75f)) }
        }
    }
}

@Composable
private fun ColumnScope.HourCard(report: StatsReport) {
    val hour = report.favoriteHour ?: return
    CardTitle(stringResource(R.string.feature_profile_wrapped_hour))
    val format = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault())
    Column {
        Text(LocalTime.of(hour, 0).format(format), style = MaterialTheme.typography.displayLarge, color = LocalWrappedAccent.current, maxLines = 1)
        Text(
            stringResource(
                when (hour) {
                    in 0..5 -> R.string.feature_profile_wrapped_hour_night
                    in 6..11 -> R.string.feature_profile_wrapped_hour_morning
                    in 12..17 -> R.string.feature_profile_wrapped_hour_afternoon
                    else -> R.string.feature_profile_wrapped_hour_evening
                },
            ),
            style = MaterialTheme.typography.headlineMedium,
        )
    }
    // A curva do dia: quanto você ouviu em cada hora, com a favorita marcada.
    DayCurve(remember(report) { smoothHourLevels(report.hourLevels) }, hour, Modifier.weight(1f).fillMaxWidth())
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf(0, 12, 23).forEach { mark ->
            Text(LocalTime.of(mark, 0).format(format), style = MaterialTheme.typography.labelMedium)
        }
    }
    Text(stringResource(R.string.feature_profile_wrapped_hour_note), style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun DayCurve(levels: List<Float>, favorite: Int, modifier: Modifier) {
    val ink = LocalContentColor.current
    val accent = LocalWrappedAccent.current
    androidx.compose.foundation.Canvas(modifier) {
        if (levels.size < 2) return@Canvas
        val inset = 10.dp.toPx()
        val top = inset
        val bottom = size.height - 2f
        fun x(i: Int) = inset + (size.width - 2 * inset) * i / (levels.size - 1)
        fun y(i: Int) = bottom - (bottom - top) * levels[i].coerceIn(0f, 1f)
        val line = androidx.compose.ui.graphics.Path().apply {
            moveTo(x(0), y(0))
            for (i in 1 until levels.size) {
                val middle = (x(i - 1) + x(i)) / 2f
                cubicTo(middle, y(i - 1), middle, y(i), x(i), y(i))
            }
        }
        val area = androidx.compose.ui.graphics.Path().apply {
            addPath(line)
            lineTo(x(levels.lastIndex), size.height)
            lineTo(x(0), size.height)
            close()
        }
        drawPath(area, ink.copy(alpha = 0.28f))
        drawPath(line, accent, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
        drawCircle(accent, radius = 8.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x(favorite), y(favorite)))
    }
}

@Composable
private fun ColumnScope.DiscoveriesCard(report: StatsReport, viewModel: MyProfileViewModel) {
    CardTitle(pluralStringResource(R.plurals.feature_profile_wrapped_discoveries, report.discoveryCount, report.discoveryCount))
    Text(stringResource(R.string.feature_profile_wrapped_discoveries_sub), style = MaterialTheme.typography.bodyLarge)
    // Grade de fotos grandes que enche a tela, duas por linha.
    Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        report.discoveries.chunked(2).forEach { pair ->
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { name ->
                    val ink = LocalContentColor.current
                    Surface(
                        color = ink.copy(alpha = 0.14f),
                        contentColor = ink,
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    ) {
                        Column(
                            Modifier.fillMaxSize().padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            ArtistAvatar(viewModel, name, 88.dp)
                            Text(
                                name,
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ColumnScope.SprintWrappedCard(report: StatsReport, viewModel: MyProfileViewModel) {
    val sprint = report.sprint ?: return
    CardTitle(stringResource(R.string.feature_profile_stats_sprint))
    val locale = Locale.getDefault()
    val rows = sprint.rows.sortedBy { it.ranks.lastOrNull() ?: Int.MAX_VALUE }
    rows.forEach { row ->
        val last = row.ranks.lastOrNull()
        val previous = row.ranks.getOrNull(row.ranks.lastIndex - 1)
        val previousMonth = sprint.months.getOrNull(sprint.months.lastIndex - 1)
        RowCard(Modifier.weight(1f).fillMaxWidth()) {
            Text(
                last?.toString() ?: stringResource(R.string.feature_profile_stats_none),
                style = MaterialTheme.typography.headlineLarge,
                color = LocalWrappedAccent.current,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.width(36.dp),
            )
            ArtistAvatar(viewModel, row.name, 56.dp)
            Column(Modifier.weight(1f)) {
                Text(row.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (previous != null && previousMonth != null) {
                    val monthName = previousMonth.month.getDisplayName(TextStyle.SHORT, locale).replaceFirstChar { it.titlecase(locale) }
                    Text("$monthName #$previous", style = MaterialTheme.typography.bodyMedium)
                }
            }
            val badge = when {
                last == null -> stringResource(R.string.feature_profile_stats_none)
                previous == null -> stringResource(R.string.feature_profile_stats_new)
                previous > last -> stringResource(R.string.feature_profile_stats_up, previous - last)
                previous < last -> stringResource(R.string.feature_profile_stats_down, last - previous)
                else -> stringResource(R.string.feature_profile_stats_none)
            }
            Surface(color = LocalWrappedAccent.current, contentColor = LocalWrappedBackdrop.current, shape = MaterialTheme.shapes.small) {
                Text(badge, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), maxLines = 1)
            }
        }
    }
}

@Composable
private fun ColumnScope.TopCard(report: StatsReport, viewModel: MyProfileViewModel, showActions: Boolean) {
    CardTitle(stringResource(R.string.feature_profile_wrapped_top))
    // A capa marca a música; a barra mostra quanto cada uma foi ouvida em relação à mais ouvida.
    val top = report.songs.take(MAX_TOP)
    val most = (top.maxOfOrNull { it.plays } ?: 1).coerceAtLeast(1)
    val ink = LocalContentColor.current
    val accent = LocalWrappedAccent.current
    top.forEach { song ->
        RowCard(Modifier.weight(1f).fillMaxWidth()) {
            Cover(viewModel, song.uri, song.title, Modifier.fillMaxHeight().aspectRatio(1f))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(song.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(
                        stringResource(R.string.feature_profile_wrapped_times_short, song.plays),
                        style = MaterialTheme.typography.titleMedium,
                        color = accent,
                        maxLines = 1,
                    )
                }
                Text(song.artist.orEmpty(), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Box(Modifier.fillMaxWidth().height(6.dp).background(ink.copy(alpha = 0.2f), MaterialTheme.shapes.extraSmall)) {
                    Box(Modifier.fillMaxWidth(song.plays.toFloat() / most).fillMaxHeight().background(accent, MaterialTheme.shapes.extraSmall))
                }
            }
        }
    }
    if (!showActions) return
    var created by remember { mutableStateOf<Int?>(null) }
    val name = stringResource(R.string.feature_profile_wrapped_playlist_name, report.to.year)
    val none = stringResource(R.string.feature_profile_wrapped_playlist_none)
    var message by remember { mutableStateOf<String?>(null) }
    val createdText = created?.let { pluralStringResource(R.plurals.feature_profile_wrapped_playlist_created, it, it) }
    Button(
        onClick = {
            viewModel.createTopPlaylist(name) { count ->
                if (count > 0) created = count else message = none
            }
        },
        enabled = created == null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(createdText ?: stringResource(R.string.feature_profile_wrapped_playlist_create))
    }
    message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
}

/** Um item do resumo: título, linha de baixo (o artista da música) e a imagem que enche o espaço dado. */
private class SummaryItem(val title: String, val subtitle: String?, val image: @Composable (Modifier) -> Unit)

/** Foto do artista preenchendo o espaço dado (a de círculo das listas não serve aqui). */
@Composable
private fun ArtistPhoto(viewModel: MyProfileViewModel, name: String, modifier: Modifier) {
    val photos = rememberImageSet(remember(name) { artistAttempts(name, SUMMARY_PHOTO_PX, viewModel) })
    BoomImageCycle(photos) { shown -> BoomArtwork(image = shown, fallbackText = name, modifier = modifier, size = null) }
}

private const val SUMMARY_PHOTO_PX = 360

/** Uma parte do resumo: o primeiro em bloco grande com o nome no destaque e os outros em linhas ao lado. */
@Composable
private fun ColumnScope.SummarySection(title: String, items: List<SummaryItem>) {
    if (items.isEmpty()) return
    Text(title, style = MaterialTheme.typography.titleLarge, color = LocalWrappedAccent.current)
    val ink = LocalContentColor.current
    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val first = items.first()
        Surface(
            color = LocalWrappedAccent.current,
            contentColor = LocalWrappedBackdrop.current,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        ) {
            Column {
                Box(Modifier.weight(1f).fillMaxWidth()) { first.image(Modifier.fillMaxSize()) }
                Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Text(first.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    first.subtitle?.let { Text(it, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items.drop(1).forEach { item ->
                Surface(
                    color = ink.copy(alpha = 0.14f),
                    contentColor = ink,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        item.image(Modifier.fillMaxHeight().aspectRatio(1f))
                        Column(Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            item.subtitle?.let { Text(it, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        }
                    }
                }
            }
        }
    }
}

/** A última tela: o resumo do período (top artistas, mais ouvidas e tempo) para compartilhar numa imagem só. */
@Composable
private fun ColumnScope.SummaryCard(report: StatsReport, viewModel: MyProfileViewModel, showActions: Boolean, onShare: () -> Unit) {
    val artists = report.artists.take(MAX_TOP).map { artist ->
        SummaryItem(artist.name, null) { modifier -> ArtistPhoto(viewModel, artist.name, modifier) }
    }
    val songs = report.songs.take(MAX_TOP).map { song ->
        SummaryItem(song.title, song.artist) { modifier -> Cover(viewModel, song.uri, song.title, modifier) }
    }
    SummarySection(stringResource(R.string.feature_profile_wrapped_summary_artists), artists)
    SummarySection(stringResource(R.string.feature_profile_wrapped_summary_songs), songs)
    Text(stringResource(R.string.feature_profile_wrapped_summary_time), style = MaterialTheme.typography.titleLarge, color = LocalWrappedAccent.current)
    Surface(color = LocalWrappedAccent.current, contentColor = LocalWrappedBackdrop.current, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.feature_profile_wrapped_minutes_value, java.text.NumberFormat.getIntegerInstance().format(report.minutes)),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
    if (!showActions) return
    Button(onClick = onShare, modifier = Modifier.fillMaxWidth()) {
        Icon(BoomIcons.Share, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.feature_profile_wrapped_share))
    }
}
