// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.profile

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.graphics.Color
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
import com.boomlistener.core.designsystem.theme.boomEffectsSpec
import com.boomlistener.data.wellbeing.StatsPeriod
import com.boomlistener.data.wellbeing.StatsReport
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/** Tempo de cada cartão antes de passar sozinho. */
private const val PAGE_MS = 7_000f
private const val TAP_BACK_SHARE = 0.35f
private const val MAX_GENRES = 5
private const val MAX_TOP = 5

private enum class WrappedCard { Intro, Artists, Song, Genres, Time, Hour, Discoveries, Sprint, Top }

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

    val current = report?.takeIf { it.period == period }
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        if (current == null) {
            BoomLoading(Modifier.fillMaxSize())
        } else {
            WrappedCards(current, viewModel, hasSong = song != null, onBack = onBack)
        }
    }
}

@Composable
private fun WrappedCards(report: StatsReport, viewModel: MyProfileViewModel, hasSong: Boolean, onBack: () -> Unit) {
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
        }
    }
    var page by remember(cards) { mutableIntStateOf(0) }
    var paused by remember { mutableStateOf(false) }
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

    val effects = boomEffectsSpec<Float>()
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(cards) {
                detectTapGestures(
                    onPress = {
                        paused = true
                        tryAwaitRelease()
                        paused = false
                    },
                    onTap = { offset ->
                        if (offset.x < size.width * TAP_BACK_SHARE) page = (page - 1).coerceAtLeast(0)
                        else page = (page + 1).coerceAtMost(cards.lastIndex)
                    },
                )
            },
    ) {
        AnimatedContent(
            targetState = page,
            transitionSpec = { fadeIn(effects) togetherWith fadeOut(effects) },
            label = "wrapped-card",
            modifier = Modifier.fillMaxSize(),
        ) { index ->
            val card = cards.getOrNull(index) ?: WrappedCard.Intro
            val (container, onContainer) = cardColors(card)
            Surface(color = container, contentColor = onContainer, modifier = Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(top = 56.dp, bottom = LocalBottomOverlay.current + 24.dp)
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    when (card) {
                        WrappedCard.Intro -> IntroCard(report, viewModel)
                        WrappedCard.Artists -> ArtistsCard(report, viewModel)
                        WrappedCard.Song -> SongCard(report, viewModel)
                        WrappedCard.Genres -> GenresCard(report)
                        WrappedCard.Time -> TimeCard(report)
                        WrappedCard.Hour -> HourCard(report)
                        WrappedCard.Discoveries -> DiscoveriesCard(report, viewModel)
                        WrappedCard.Sprint -> SprintWrappedCard(report)
                        WrappedCard.Top -> TopCard(report, viewModel)
                    }
                }
            }
        }
        // Barras de progresso e fechar, por cima de tudo.
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                cards.indices.forEach { index ->
                    val value by remember(index, page) {
                        derivedStateOf { if (index < page) 1f else if (index == page) progress.floatValue else 0f }
                    }
                    LinearProgressIndicator(
                        progress = { value },
                        modifier = Modifier.weight(1f).height(4.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onBack) {
                    Icon(BoomIcons.Close, contentDescription = stringResource(R.string.feature_profile_wrapped_close))
                }
            }
        }
    }
}

/** Cada cartão tem o seu par de cores do tema (fundo e texto). */
@Composable
private fun cardColors(card: WrappedCard): Pair<Color, Color> {
    val colors = MaterialTheme.colorScheme
    return when (card) {
        WrappedCard.Intro, WrappedCard.Hour -> colors.primaryContainer to colors.onPrimaryContainer
        WrappedCard.Song, WrappedCard.Top -> colors.tertiaryContainer to colors.onTertiaryContainer
        WrappedCard.Time, WrappedCard.Discoveries -> colors.secondaryContainer to colors.onSecondaryContainer
        WrappedCard.Artists, WrappedCard.Genres, WrappedCard.Sprint -> colors.surfaceContainerHigh to colors.onSurface
    }
}

@Composable
private fun CardTitle(text: String) {
    Text(text, style = MaterialTheme.typography.headlineMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
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

@Composable
private fun ColumnScope.IntroCard(report: StatsReport, viewModel: MyProfileViewModel) {
    Text(periodLabel(report), style = MaterialTheme.typography.labelLarge)
    CardTitle(stringResource(R.string.feature_profile_wrapped_intro))
    // Capas dos álbuns mais ouvidos (ou das músicas, sem álbum), em mosaico.
    val covers = remember(report) {
        report.albums.map { it.coverUri to it.album }.ifEmpty { report.songs.map { it.uri to it.title } }.take(6)
    }
    Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        covers.chunked(2).forEach { pair ->
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { (uri, title) -> Cover(viewModel, uri, title, Modifier.weight(1f).fillMaxHeight()) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ColumnScope.ArtistsCard(report: StatsReport, viewModel: MyProfileViewModel) {
    CardTitle(stringResource(R.string.feature_profile_wrapped_artists))
    val top = report.artists.take(MAX_TOP)
    top.firstOrNull()?.let { first ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ArtistAvatar(viewModel, first.name, 112.dp)
            Column(Modifier.weight(1f)) {
                Text(first.name, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(playsAndTime(first.plays, first.minutes), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
    top.drop(1).forEachIndexed { index, artist ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text((index + 2).toString(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
            ArtistAvatar(viewModel, artist.name, 48.dp)
            Column(Modifier.weight(1f)) {
                Text(artist.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    pluralStringResource(R.plurals.feature_profile_plays, artist.plays, artist.plays),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.SongCard(report: StatsReport, viewModel: MyProfileViewModel) {
    // A música que toca aqui: a mais ouvida, ou a seguinte se a mais ouvida saiu do aparelho.
    val playing by viewModel.wrappedSong.collectAsStateWithLifecycle()
    val song = playing ?: return
    val isTop = report.songs.firstOrNull()?.uri == song.uri
    CardTitle(
        stringResource(if (isTop) R.string.feature_profile_wrapped_song else R.string.feature_profile_wrapped_song_fallback),
    )
    Cover(viewModel, song.uri, song.title, Modifier.fillMaxWidth().weight(1f, fill = false).aspectRatio(1f))
    Text(song.title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
    song.artist?.let { Text(it, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    Text(playsAndTime(song.plays, song.minutes), style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun ColumnScope.GenresCard(report: StatsReport) {
    CardTitle(stringResource(R.string.feature_profile_wrapped_genres))
    val colors = MaterialTheme.colorScheme
    val palette = listOf(
        colors.primary to colors.onPrimary,
        colors.secondary to colors.onSecondary,
        colors.tertiary to colors.onTertiary,
        colors.inverseSurface to colors.inverseOnSurface,
        colors.primaryContainer to colors.onPrimaryContainer,
    )
    Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        report.genres.take(MAX_GENRES).forEachIndexed { index, genre ->
            val (fill, on) = palette[index % palette.size]
            Surface(
                color = fill,
                contentColor = on,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth().weight(genre.share.coerceAtLeast(0.12f)),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.Center) {
                    Text(
                        stringResource(R.string.feature_profile_stats_percent, (genre.share * 100).toInt()),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(genre.genre, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
    Column {
        Text(report.genreCount.toString(), style = MaterialTheme.typography.headlineSmall)
        Text(pluralStringResource(R.plurals.feature_profile_stats_genres, report.genreCount), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ColumnScope.TimeCard(report: StatsReport) {
    CardTitle(stringResource(R.string.feature_profile_wrapped_time))
    Text(minutesText(report.minutes), style = MaterialTheme.typography.displayMedium, maxLines = 2)
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        report.bestDay?.let { best ->
            Highlight(stringResource(R.string.feature_profile_stats_record_day), best.date.format(DateShown), minutesText(best.minutes))
        }
        if (report.longestStreak > 1) {
            Highlight(
                stringResource(R.string.feature_profile_stats_streak),
                pluralStringResource(R.plurals.feature_profile_stats_day_count, report.longestStreak, report.longestStreak),
                null,
            )
        }
        report.mostArtistsDay?.let { most ->
            Highlight(
                stringResource(R.string.feature_profile_stats_most_artists_day),
                pluralStringResource(R.plurals.feature_profile_stats_artist_count, most.artists, most.artists),
                most.date.format(DateShown),
            )
        }
    }
}

@Composable
private fun ColumnScope.HourCard(report: StatsReport) {
    val hour = report.favoriteHour ?: return
    CardTitle(stringResource(R.string.feature_profile_wrapped_hour))
    val shown = LocalTime.of(hour, 0).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault()))
    Text(shown, style = MaterialTheme.typography.displayLarge)
    Text(
        stringResource(
            when (hour) {
                in 0..5 -> R.string.feature_profile_wrapped_hour_night
                in 6..11 -> R.string.feature_profile_wrapped_hour_morning
                in 12..17 -> R.string.feature_profile_wrapped_hour_afternoon
                else -> R.string.feature_profile_wrapped_hour_evening
            },
        ),
        style = MaterialTheme.typography.headlineSmall,
    )
    Text(stringResource(R.string.feature_profile_wrapped_hour_note), style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun ColumnScope.DiscoveriesCard(report: StatsReport, viewModel: MyProfileViewModel) {
    CardTitle(pluralStringResource(R.plurals.feature_profile_wrapped_discoveries, report.discoveryCount, report.discoveryCount))
    Text(stringResource(R.string.feature_profile_wrapped_discoveries_sub), style = MaterialTheme.typography.bodyLarge)
    report.discoveries.forEach { name ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ArtistAvatar(viewModel, name, 56.dp)
            Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ColumnScope.SprintWrappedCard(report: StatsReport) {
    val sprint = report.sprint ?: return
    CardTitle(stringResource(R.string.feature_profile_stats_sprint))
    Text(stringResource(R.string.feature_profile_stats_sprint_sub), style = MaterialTheme.typography.bodyMedium)
    val locale = Locale.getDefault()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.weight(1f))
        sprint.months.forEach { month ->
            Text(
                month.month.getDisplayName(TextStyle.SHORT, locale).replaceFirstChar { it.titlecase(locale) },
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.width(40.dp),
            )
        }
    }
    sprint.rows.forEach { row ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(row.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            row.ranks.forEach { rank ->
                Text(
                    rank?.toString() ?: stringResource(R.string.feature_profile_stats_none),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(40.dp),
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.TopCard(report: StatsReport, viewModel: MyProfileViewModel) {
    CardTitle(stringResource(R.string.feature_profile_wrapped_top))
    report.songs.take(MAX_TOP).forEachIndexed { index, song ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text((index + 1).toString(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
            Cover(viewModel, song.uri, song.title, Modifier.size(56.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist.orEmpty(), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(pluralStringResource(R.plurals.feature_profile_plays, song.plays, song.plays), style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
    Spacer(Modifier.weight(1f))
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
