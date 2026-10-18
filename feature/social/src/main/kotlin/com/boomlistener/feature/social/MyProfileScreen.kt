// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.social

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.core.designsystem.component.BoomArtwork
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.designsystem.component.BoomLoading
import com.boomlistener.core.designsystem.component.BoomScreenScaffold
import com.boomlistener.core.designsystem.component.artworkTintedBackground
import com.boomlistener.core.designsystem.component.rememberArtworkColor
import com.boomlistener.data.wellbeing.HistoryEntry
import com.boomlistener.data.wellbeing.SongStat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val CoverCard = 136.dp

/**
 * O seu perfil, no estilo de um "cartão de ouvinte": cabeçalho no tom da sua
 * música mais ouvida, os números do mês, o que mais tocou, quando você ouve,
 * seus artistas e o histórico. Tudo calculado no celular.
 */
@Composable
internal fun MyProfileScreen(
    viewModel: MyProfileViewModel,
    onBack: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenHistory: () -> Unit,
    onCreateProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val saveFailed by viewModel.saveFailed.collectAsStateWithLifecycle()
    var renaming by remember { mutableStateOf(false) }

    BoomScreenScaffold(
        title = stringResource(R.string.feature_social_my_profile),
        onBack = onBack,
        modifier = modifier,
        actions = {
            if (state?.account != null) {
                IconButton(onClick = { renaming = true }) {
                    Icon(BoomIcons.Edit, contentDescription = stringResource(R.string.feature_social_edit_name))
                }
            }
        },
    ) { padding ->
        val current = state
        if (current == null) {
            BoomLoading(Modifier.padding(padding))
            return@BoomScreenScaffold
        }
        LazyColumn(contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 32.dp)) {
            item(key = "header") {
                Header(current, viewModel, onCreateProfile, onToggleSharing = viewModel::setSharing)
                if (saveFailed) {
                    Text(
                        stringResource(R.string.feature_social_offline),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }
            item(key = "month") { MonthCard(current) }

            if (current.topSongs.isNotEmpty()) {
                item(key = "top") {
                    SectionTitle(stringResource(R.string.feature_social_most_played), stringResource(R.string.feature_social_most_played_sub))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                        itemsIndexed(current.topSongs, key = { _, song -> song.uri }) { index, song ->
                            TopSongCard(index + 1, song, viewModel, onClick = { viewModel.playTop(index) })
                        }
                    }
                }
            }

            item(key = "time") {
                SectionTitle(stringResource(R.string.feature_social_listening_time), stringResource(R.string.feature_social_last_7_days))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                    item { WeekCard(current) }
                    item { DayPartsCard(current) }
                }
            }

            if (current.stats.topArtists.isNotEmpty()) {
                item(key = "artists") {
                    SectionTitle(stringResource(R.string.feature_social_top_artists), stringResource(R.string.feature_social_last_30_days))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                        items(current.stats.topArtists, key = { it.name }) { artist ->
                            PersonChip(
                                name = artist.name,
                                caption = pluralStringResource(R.plurals.feature_social_plays, artist.plays, artist.plays),
                                onClick = { onOpenArtist(artist.name) },
                            )
                        }
                    }
                }
            }

            if (current.recent.isNotEmpty()) {
                item(key = "history") {
                    SectionTitle(
                        stringResource(R.string.feature_social_history),
                        null,
                        onMore = onOpenHistory,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                        items(current.recent, key = { "${it.timestampMs}-${it.uri}" }) { entry ->
                            HistoryCard(entry, current.today, viewModel, onClick = { viewModel.playRecent(entry) })
                        }
                    }
                }
            }

            item(key = "following") {
                SectionTitle(
                    stringResource(R.string.feature_social_following_count, current.following.size),
                    null,
                )
                if (current.following.isEmpty()) {
                    Text(
                        stringResource(R.string.feature_social_following_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                        items(current.following, key = { it.deezerId }) { artist ->
                            PersonChip(name = artist.name, caption = null, onClick = { onOpenArtist(artist.name) })
                        }
                    }
                }
            }
        }
    }

    val account = state?.account
    if (renaming && account != null) {
        var name by remember { mutableStateOf(account.name) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text(stringResource(R.string.feature_social_edit_name)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    singleLine = true,
                    label = { Text(stringResource(R.string.feature_social_name)) },
                )
            },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = {
                    renaming = false
                    viewModel.rename(name)
                }) { Text(stringResource(R.string.feature_social_save)) }
            },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text(stringResource(R.string.feature_social_cancel)) } },
        )
    }
}

/** Cabeçalho tingido pela capa da sua mais ouvida, com foto, nome, @ e o compartilhamento. */
@Composable
private fun Header(
    state: MyProfileUiState,
    viewModel: MyProfileViewModel,
    onCreateProfile: () -> Unit,
    onToggleSharing: (Boolean) -> Unit,
) {
    val coverUri = state.topSongs.firstOrNull()?.uri
    val cover = rememberLocalArtwork(viewModel, coverUri, 256.dp)
    val seed = rememberArtworkColor(cover)
    val account = state.account
    Column(
        Modifier
            .fillMaxWidth()
            .artworkTintedBackground(seed)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(account?.name ?: "?", size = 72.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    account?.name ?: stringResource(R.string.feature_social_you),
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    account?.let { "@" + it.handle } ?: stringResource(R.string.feature_social_no_profile_yet),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        if (account == null) {
            FilledTonalButton(onClick = onCreateProfile) { Text(stringResource(R.string.feature_social_create_profile)) }
        } else {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        stringResource(R.string.feature_social_share_title),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = account.sharing, onCheckedChange = onToggleSharing)
                }
            }
        }
    }
}

/** "Este mês": músicas, horas e artistas. */
@Composable
private fun MonthCard(state: MyProfileUiState) {
    val month = state.today.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
        .replaceFirstChar { it.titlecase(Locale.getDefault()) }
    val hours = state.stats.monthMinutes / 60
    val minutes = state.stats.monthMinutes % 60
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(BoomIcons.History, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.feature_social_this_month).uppercase(), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.weight(1f))
                Text(month, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigNumber(state.stats.monthPlays.toString(), pluralStringResource(R.plurals.feature_social_songs, state.stats.monthPlays), Modifier.weight(1f))
                BigNumber(
                    if (hours > 0) stringResource(R.string.feature_social_hours_minutes, hours, minutes) else stringResource(R.string.feature_social_minutes, minutes),
                    stringResource(R.string.feature_social_listened),
                    Modifier.weight(1f),
                )
                BigNumber(state.stats.monthArtists.toString(), pluralStringResource(R.plurals.feature_social_artists, state.stats.monthArtists), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BigNumber(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.headlineMedium, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String?, onMore: (() -> Unit)? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onMore != null) Modifier.clickable(onClick = onMore) else Modifier)
            .padding(start = 20.dp, end = 12.dp, top = 24.dp, bottom = 10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (onMore != null) {
            Text(stringResource(R.string.feature_social_see_all), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun TopSongCard(rank: Int, song: SongStat, viewModel: MyProfileViewModel, onClick: () -> Unit) {
    val cover = rememberLocalArtwork(viewModel, song.uri, CoverCard)
    Column(Modifier.width(CoverCard).clip(MaterialTheme.shapes.large).clickable(onClick = onClick)) {
        Box {
            BoomArtwork(image = cover, fallbackText = song.title, size = CoverCard)
            // Posição no ranking, num selo no canto.
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.padding(8.dp).size(28.dp),
            ) {
                Box(contentAlignment = Alignment.Center) { Text(rank.toString(), style = MaterialTheme.typography.labelLarge) }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(song.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            pluralStringResource(R.plurals.feature_social_plays, song.plays, song.plays),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Barras dos últimos 7 dias; o dia que você mais ouviu fica em destaque. */
@Composable
private fun WeekCard(state: MyProfileUiState) {
    val week = state.stats.week
    val max = (week.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(1)
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.extraLarge) {
        Column(Modifier.width(300.dp).padding(16.dp)) {
            Text(stringResource(R.string.feature_social_daily), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.height(140.dp)) {
                week.forEach { day ->
                    val isMax = day.minutes == max && day.minutes > 0
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(
                            if (day.minutes > 0) shortDuration(day.minutes) else "",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                        )
                        Spacer(Modifier.height(4.dp))
                        val fraction = day.minutes.toFloat() / max
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(4.dp + (80 * fraction).dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(
                                    if (isMax) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                ),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (day.date == state.today) stringResource(R.string.feature_social_today_short)
                            else day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun shortDuration(minutes: Int): String =
    if (minutes >= 60) stringResource(R.string.feature_social_hours_short, minutes / 60, minutes % 60)
    else stringResource(R.string.feature_social_minutes, minutes)

/** Manhã, tarde, noite e madrugada: quando você mais ouve. */
@Composable
private fun DayPartsCard(state: MyProfileUiState) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.extraLarge) {
        Column(Modifier.width(240.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.feature_social_peak_hours), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            listOf(
                DayPart.Morning to R.string.feature_social_morning,
                DayPart.Afternoon to R.string.feature_social_afternoon,
                DayPart.Night to R.string.feature_social_night,
                DayPart.Dawn to R.string.feature_social_dawn,
            ).forEach { (part, label) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(label), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(96.dp))
                    LinearProgressIndicator(
                        progress = { state.stats.dayParts[part] ?: 0f },
                        modifier = Modifier.weight(1f),
                        drawStopIndicator = {},
                    )
                }
            }
        }
    }
}

/** Bolinha com nome embaixo (artista). */
@Composable
private fun PersonChip(name: String, caption: String?, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(88.dp).clip(MaterialTheme.shapes.large).clickable(onClick = onClick).padding(vertical = 4.dp),
    ) {
        Avatar(name, size = 72.dp)
        Spacer(Modifier.height(6.dp))
        Text(name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        caption?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun HistoryCard(entry: HistoryEntry, today: LocalDate, viewModel: MyProfileViewModel, onClick: () -> Unit) {
    val cover = rememberLocalArtwork(viewModel, entry.uri, CoverCard)
    val date = remember(entry.timestampMs) { Instant.ofEpochMilli(entry.timestampMs).atZone(ZoneId.systemDefault()).toLocalDate() }
    val label = when (date) {
        today -> stringResource(R.string.feature_social_today)
        today.minusDays(1) -> stringResource(R.string.feature_social_yesterday)
        else -> date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
    }
    Column(Modifier.width(CoverCard).clip(MaterialTheme.shapes.large).clickable(onClick = onClick)) {
        Box(contentAlignment = Alignment.BottomStart) {
            BoomArtwork(image = cover, fallbackText = entry.title, size = CoverCard)
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape,
                modifier = Modifier.padding(8.dp),
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(entry.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            entry.artist.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun rememberLocalArtwork(viewModel: MyProfileViewModel, uri: String?, size: Dp): ImageBitmap? {
    val px = with(LocalDensity.current) { size.roundToPx() }
    val image by produceState<ImageBitmap?>(initialValue = null, uri, px) {
        value = uri?.let { viewModel.artwork(it, px)?.asImageBitmap() }
    }
    return image
}
