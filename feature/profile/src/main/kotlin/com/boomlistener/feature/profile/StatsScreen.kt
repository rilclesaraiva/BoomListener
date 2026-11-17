// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.profile

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.core.designsystem.component.BoomArtwork
import com.boomlistener.core.designsystem.component.BoomImageCycle
import com.boomlistener.core.designsystem.component.BoomLoading
import com.boomlistener.core.designsystem.component.BoomPageTitle
import com.boomlistener.core.designsystem.component.BoomScreenScaffold
import com.boomlistener.core.designsystem.component.LocalBottomOverlay
import com.boomlistener.core.designsystem.component.clickableWithMenu
import com.boomlistener.core.designsystem.component.rememberImageSet
import com.boomlistener.data.wellbeing.AlbumRank
import com.boomlistener.data.wellbeing.ArtistRank
import com.boomlistener.data.wellbeing.GenreRank
import com.boomlistener.data.wellbeing.RankChange
import com.boomlistener.data.wellbeing.SongRank
import com.boomlistener.data.wellbeing.Sprint
import com.boomlistener.data.wellbeing.StatsPeriod
import com.boomlistener.data.wellbeing.StatsReport
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

private const val PREVIEW_COUNT = 5

/**
 * Estatísticas completas: o período escolhido, os números, os destaques e o Top
 * 5 (ou Top 100) de artistas, músicas, álbuns e gêneros. Tudo calculado no
 * aparelho a partir do resumo diário.
 */
@Composable
internal fun StatsScreen(
    viewModel: MyProfileViewModel,
    onBack: () -> Unit,
    onOpenArtist: (String) -> Unit,
    songMenu: ProfileSongMenu,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val report by viewModel.report.collectAsStateWithLifecycle()
    val period by viewModel.period.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    BoomScreenScaffold(
        title = stringResource(R.string.feature_profile_stats_title),
        onBack = onBack,
        modifier = modifier,
        topBar = {},
    ) { padding ->
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 32.dp + LocalBottomOverlay.current),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "title") {
                BoomPageTitle(stringResource(R.string.feature_profile_stats_title), hasBack = true)
            }
            item(key = "periods") { PeriodChips(period, viewModel::selectPeriod) }

            val current = report
            if (current == null) {
                item(key = "loading") { BoomLoading(Modifier.fillMaxWidth().height(240.dp).padding(padding)) }
                return@LazyColumn
            }
            if (current.plays == 0 && current.minutes == 0) {
                item(key = "empty") {
                    Column(Modifier.padding(horizontal = 24.dp, vertical = 32.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.feature_profile_stats_empty_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.feature_profile_stats_empty_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                item(key = "summary") { SummaryCard(current) }
                if (current.bestDay != null || current.longestStreak > 1 || current.mostArtistsDay != null) {
                    item(key = "highlights") { HighlightsCard(current) }
                }
                rankedSection(
                    key = "artists",
                    title = stringResource(R.string.feature_profile_stats_top_artists),
                    items = current.artists,
                    period = period,
                ) { index, artist -> ArtistRow(index + 1, artist, viewModel, onOpenArtist) }
                rankedSection(
                    key = "songs",
                    title = stringResource(R.string.feature_profile_stats_top_songs),
                    items = current.songs,
                    period = period,
                ) { index, song -> SongRow(index + 1, song, viewModel, songMenu) }
                rankedSection(
                    key = "albums",
                    title = stringResource(R.string.feature_profile_stats_top_albums),
                    items = current.albums,
                    period = period,
                ) { index, album -> AlbumRow(index + 1, album, viewModel) }
                rankedSection(
                    key = "genres",
                    title = stringResource(R.string.feature_profile_stats_top_genres),
                    items = current.genres,
                    period = period,
                ) { index, genre -> GenreRow(index + 1, genre, maxShare = current.genres.firstOrNull()?.share ?: 1f) }
                current.sprint?.let { sprint ->
                    item(key = "sprint") {
                        SectionTitle(stringResource(R.string.feature_profile_stats_sprint), stringResource(R.string.feature_profile_stats_sprint_sub))
                        SprintCard(sprint)
                    }
                }
            }
            state?.let { profile ->
                item(key = "week") {
                    SectionTitle(stringResource(R.string.feature_profile_listening_time), stringResource(R.string.feature_profile_last_7_days))
                    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        WeekCard(profile)
                        DayPartsCard(profile)
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodChips(selected: StatsPeriod, onSelect: (StatsPeriod) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        StatsPeriod.entries.forEach { period ->
            FilterChip(
                selected = period == selected,
                onClick = { onSelect(period) },
                label = {
                    Text(
                        stringResource(
                            when (period) {
                                StatsPeriod.Last4Weeks -> R.string.feature_profile_stats_period_4weeks
                                StatsPeriod.ThisMonth -> R.string.feature_profile_stats_period_month
                                StatsPeriod.ThisYear -> R.string.feature_profile_stats_period_year
                                StatsPeriod.AllTime -> R.string.feature_profile_stats_period_all
                            },
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun minutesText(minutes: Int): String =
    if (minutes >= 60) stringResource(R.string.feature_profile_hours_minutes, minutes / 60, minutes % 60)
    else stringResource(R.string.feature_profile_minutes, minutes)

@Composable
private fun SummaryCard(report: StatsReport) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column {
                Text(minutesText(report.minutes), style = MaterialTheme.typography.displaySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val days = report.minutes / (60 * 24)
                val hours = report.minutes / 60 % 24
                Text(
                    if (days > 0) stringResource(R.string.feature_profile_stats_days_hours, days, hours) else stringResource(R.string.feature_profile_listened),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigStat(report.plays.toString(), pluralStringResource(R.plurals.feature_profile_songs, report.plays), Modifier.weight(1f))
                BigStat(report.artistCount.toString(), pluralStringResource(R.plurals.feature_profile_artists, report.artistCount), Modifier.weight(1f))
                BigStat(report.genreCount.toString(), pluralStringResource(R.plurals.feature_profile_stats_genres, report.genreCount), Modifier.weight(1f))
                BigStat(report.activeDays.toString(), pluralStringResource(R.plurals.feature_profile_stats_days, report.activeDays), Modifier.weight(1f))
            }
            if (report.hasEstimated) {
                Text(stringResource(R.string.feature_profile_stats_estimated), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun BigStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private val DateShown: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

@Composable
private fun HighlightsCard(report: StatsReport) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
}

@Composable
private fun Highlight(label: String, value: String, detail: String?) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
        detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

/** Título + os 5 primeiros; com mais de 5, "Ver Top 100" abre a lista toda. */
private fun <T> LazyListScope.rankedSection(
    key: String,
    title: String,
    items: List<T>,
    period: StatsPeriod,
    row: @Composable (Int, T) -> Unit,
) {
    if (items.isEmpty()) return
    item(key = "$key-title") { SectionTitle(title, null) }
    item(key = "$key-rows-$period") {
        var all by remember(period) { mutableStateOf(false) }
        Column {
            items.take(if (all) items.size else PREVIEW_COUNT).forEachIndexed { index, item -> row(index, item) }
            if (items.size > PREVIEW_COUNT) {
                TextButton(onClick = { all = !all }, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text(
                        if (all) stringResource(R.string.feature_profile_stats_show_less)
                        else stringResource(R.string.feature_profile_stats_show_all, items.size),
                    )
                }
            }
        }
    }
}

@Composable
private fun RankNumber(rank: Int) {
    Text(
        rank.toString(),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.width(32.dp),
    )
}

@Composable
private fun ChangeBadge(change: RankChange) {
    val text = when (change) {
        is RankChange.Up -> stringResource(R.string.feature_profile_stats_up, change.places)
        is RankChange.Down -> stringResource(R.string.feature_profile_stats_down, change.places)
        RankChange.New -> stringResource(R.string.feature_profile_stats_new)
        RankChange.Same, RankChange.None -> return
    }
    val color = when (change) {
        is RankChange.Down -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    Text(text, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1)
}

@Composable
private fun playsAndTime(plays: Int, minutes: Int): String =
    stringResource(
        R.string.feature_profile_stats_plays_time,
        pluralStringResource(R.plurals.feature_profile_plays, plays, plays),
        minutesText(minutes),
    )

@Composable
private fun ArtistRow(rank: Int, artist: ArtistRank, viewModel: MyProfileViewModel, onOpenArtist: (String) -> Unit) {
    val px = with(LocalDensity.current) { 48.dp.roundToPx() }
    val photos = rememberImageSet(remember(artist.name, px) { artistAttempts(artist.name, px, viewModel) })
    val menuOpen = remember { mutableStateOf(false) }
    ProfileArtistMenuSheet(
        menuOpen,
        artist.name,
        photos.firstOrNull(),
        onPlay = { viewModel.playArtist(artist.name, shuffled = false) },
        onShuffle = { viewModel.playArtist(artist.name, shuffled = true) },
        onOpenArtist = { onOpenArtist(artist.name) },
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickableWithMenu(menuOpen, onClick = { onOpenArtist(artist.name) }).padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        RankNumber(rank)
        BoomImageCycle(photos) { shown -> Avatar(artist.name, size = 48.dp, image = shown) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(artist.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                playsAndTime(artist.plays, artist.minutes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        ChangeBadge(artist.change)
    }
}

@Composable
private fun SongRow(rank: Int, song: SongRank, viewModel: MyProfileViewModel, menu: ProfileSongMenu) {
    val cover = rememberLocalArtwork(viewModel, song.uri, 48.dp)
    val menuOpen = remember { mutableStateOf(false) }
    val play = { viewModel.playReportSongs(viewModel.report.value?.songs?.indexOfFirst { it.uri == song.uri } ?: -1) }
    ProfileSongMenuSheet(menuOpen, song.uri, song.title, song.artist, cover, menu, onPlay = play)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickableWithMenu(menuOpen, onClick = play).padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        RankNumber(rank)
        BoomArtwork(image = cover, fallbackText = song.title, size = 48.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                song.artist.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            pluralStringResource(R.plurals.feature_profile_plays, song.plays, song.plays),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun AlbumRow(rank: Int, album: AlbumRank, viewModel: MyProfileViewModel) {
    val cover = rememberLocalArtwork(viewModel, album.coverUri, 48.dp)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
        RankNumber(rank)
        BoomArtwork(image = cover, fallbackText = album.album, size = 48.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(album.album, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                album.artist.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            pluralStringResource(R.plurals.feature_profile_plays, album.plays, album.plays),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun GenreRow(rank: Int, genre: GenreRank, maxShare: Float) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
        RankNumber(rank)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(genre.genre, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(
                    stringResource(R.string.feature_profile_stats_percent, (genre.share * 100).toInt().coerceAtLeast(if (genre.plays > 0) 1 else 0)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(progress = { if (maxShare > 0f) genre.share / maxShare else 0f }, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** O Top 5 do período, mês a mês: cada coluna é um mês e o número é a posição do artista nele. */
@Composable
private fun SprintCard(sprint: Sprint) {
    val locale = Locale.getDefault()
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                sprint.months.forEach { month ->
                    Text(
                        month.month.getDisplayName(TextStyle.SHORT, locale).replaceFirstChar { it.titlecase(locale) },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            style = MaterialTheme.typography.titleSmall,
                            color = if (rank == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(40.dp).clip(MaterialTheme.shapes.small),
                        )
                    }
                }
            }
        }
    }
}
