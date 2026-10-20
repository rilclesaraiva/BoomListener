// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.releases

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.core.designsystem.component.BoomArtwork
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.designsystem.component.BoomLoading
import com.boomlistener.core.designsystem.component.BoomPlaceholder
import com.boomlistener.core.designsystem.component.BoomScreenScaffold
import com.boomlistener.core.designsystem.component.ScrollToTopOnReselect
import com.boomlistener.core.model.ExternalArtist
import com.boomlistener.core.model.Release
import com.boomlistener.core.model.ReleaseType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val TileCover = 136.dp

@Composable
internal fun ReleasesScreen(
    viewModel: ReleasesViewModel,
    onOpenAlbum: (albumId: Long, artistName: String) -> Unit,
    onOpenArtist: (artistName: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    ScrollToTopOnReselect(RELEASES_ROUTE, listState)

    BoomScreenScaffold(
        title = stringResource(R.string.feature_releases_title),
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        when (val current = state) {
            ReleasesUiState.Loading -> BoomLoading(Modifier.padding(padding))
            ReleasesUiState.NoFollows -> BoomPlaceholder(
                title = stringResource(R.string.feature_releases_no_follows_title),
                message = stringResource(R.string.feature_releases_no_follows_message),
                modifier = Modifier.padding(padding),
            )
            ReleasesUiState.Failed -> BoomPlaceholder(
                title = stringResource(R.string.feature_releases_failed_title),
                message = stringResource(R.string.feature_releases_failed_message),
                modifier = Modifier.padding(padding),
                actions = { TextButton(onClick = viewModel::refresh) { Text(stringResource(R.string.feature_releases_retry)) } },
            )
            is ReleasesUiState.Ready -> LazyColumn(state = listState, contentPadding = padding) {
                item(key = "filters") { Filters(current, viewModel) }
                if (current.failed > 0) {
                    item(key = "partial") {
                        Text(
                            text = stringResource(R.string.feature_releases_partial, current.failed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        )
                    }
                }
                val empty = if (current.view == ReleasesView.Latest) current.latest.isEmpty() else current.byArtist.isEmpty()
                if (empty) {
                    item(key = "empty") {
                        Text(
                            text = stringResource(R.string.feature_releases_empty_filter),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp),
                        )
                    }
                }
                when (current.view) {
                    ReleasesView.Latest -> items(current.latest, key = { "r:" + it.release.id }) { item ->
                        LatestRow(
                            item = item,
                            today = current.today,
                            viewModel = viewModel,
                            onClick = { onOpenAlbum(item.release.id, item.artist.name) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                    ReleasesView.ByArtist -> items(current.byArtist, key = { "a:" + it.artist.id }) { entry ->
                        ArtistSection(
                            entry = entry,
                            today = current.today,
                            viewModel = viewModel,
                            onOpenArtist = { onOpenArtist(entry.artist.name) },
                            onOpenAlbum = { release -> onOpenAlbum(release.id, entry.artist.name) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Pílulas: como ver (mais recentes / por artista), o que ver (álbuns, EPs,
 * singles) e até quando (último mês, 6 meses, 1 ano).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Filters(state: ReleasesUiState.Ready, viewModel: ReleasesViewModel) {
    // As pílulas quebram linha em vez de sumir na borda da tela.
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReleasesView.entries.forEach { option ->
                Pill(
                    label = stringResource(
                        when (option) {
                            ReleasesView.Latest -> R.string.feature_releases_view_latest
                            ReleasesView.ByArtist -> R.string.feature_releases_view_by_artist
                        },
                    ),
                    selected = state.view == option,
                    onClick = { viewModel.setView(option) },
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReleaseFilter.entries.forEach { option ->
                Pill(
                    label = stringResource(
                        when (option) {
                            ReleaseFilter.All -> R.string.feature_releases_filter_all
                            ReleaseFilter.Albums -> R.string.feature_releases_filter_albums
                            ReleaseFilter.EPs -> R.string.feature_releases_filter_eps
                            ReleaseFilter.Singles -> R.string.feature_releases_filter_singles
                        },
                    ),
                    selected = state.filter == option,
                    onClick = { viewModel.setFilter(option) },
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReleasePeriod.entries.forEach { option ->
                Pill(
                    label = stringResource(
                        when (option) {
                            ReleasePeriod.Month -> R.string.feature_releases_period_month
                            ReleasePeriod.HalfYear -> R.string.feature_releases_period_half_year
                            ReleasePeriod.Year -> R.string.feature_releases_period_year
                        },
                    ),
                    selected = state.period == option,
                    onClick = { viewModel.setPeriod(option) },
                )
            }
        }
    }
}

@Composable
private fun Pill(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        leadingIcon = if (selected) {
            { Icon(BoomIcons.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
        } else {
            null
        },
        shape = CircleShape,
    )
}

@Composable
private fun LatestRow(
    item: ReleaseItem,
    today: LocalDate,
    viewModel: ReleasesViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cover = rememberDeezerImage(viewModel, item.release.coverUrl, 64.dp)
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        leadingContent = { BoomArtwork(image = cover, fallbackText = item.release.title, size = 64.dp) },
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.release.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isNewRelease(item.release, today)) {
                    Spacer(Modifier.width(8.dp))
                    NewBadge()
                }
            }
        },
        supportingContent = {
            Text(
                text = listOfNotNull(item.artist.name, typeLabel(item.release.type), formatDate(item.release))
                    .joinToString(" · "),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

@Composable
private fun ArtistSection(
    entry: ArtistReleases,
    today: LocalDate,
    viewModel: ReleasesViewModel,
    onOpenArtist: () -> Unit,
    onOpenAlbum: (Release) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(bottom = 12.dp)) {
        ArtistHeader(entry.artist, viewModel, onOpenArtist)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            items(entry.releases, key = { it.id }) { release ->
                ReleaseTile(release, today, viewModel, onClick = { onOpenAlbum(release) })
            }
        }
    }
}

@Composable
private fun ArtistHeader(artist: ExternalArtist, viewModel: ReleasesViewModel, onClick: () -> Unit) {
    val photo = rememberDeezerImage(viewModel, artist.pictureUrl, 40.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        BoomArtwork(image = photo, fallbackText = artist.name, size = 40.dp, modifier = Modifier.clip(CircleShape))
        Spacer(Modifier.width(12.dp))
        Text(artist.name, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ReleaseTile(release: Release, today: LocalDate, viewModel: ReleasesViewModel, onClick: () -> Unit) {
    val cover = rememberDeezerImage(viewModel, release.coverUrl, TileCover)
    Column(
        Modifier
            .width(TileCover)
            // Canto menor e folga embaixo: o arredondado do toque não corta as letras.
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(bottom = 8.dp),
    ) {
        BoomArtwork(image = cover, fallbackText = release.title, size = TileCover)
        Spacer(Modifier.height(6.dp))
        Text(
            release.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(
                text = listOfNotNull(typeLabel(release.type), release.year).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (isNewRelease(release, today)) {
                Spacer(Modifier.width(6.dp))
                NewBadge()
            }
        }
    }
}

/** Etiqueta "Novo" para lançamentos dos últimos 30 dias. */
@Composable
private fun NewBadge() {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = CircleShape,
    ) {
        Text(
            text = stringResource(R.string.feature_releases_new),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun typeLabel(type: ReleaseType): String = stringResource(
    when (type) {
        ReleaseType.Album -> R.string.feature_releases_type_album
        ReleaseType.EP -> R.string.feature_releases_type_ep
        ReleaseType.Single -> R.string.feature_releases_type_single
        ReleaseType.Compilation -> R.string.feature_releases_type_compilation
    },
)

@Composable
private fun formatDate(release: Release): String? = remember(release.releaseDate) {
    release.releaseDate?.let {
        runCatching { LocalDate.parse(it).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) }.getOrNull()
    } ?: release.year
}

@Composable
private fun rememberDeezerImage(viewModel: ReleasesViewModel, url: String?, size: androidx.compose.ui.unit.Dp): ImageBitmap? {
    val px = with(LocalDensity.current) { size.roundToPx() }
    val image by produceState<ImageBitmap?>(initialValue = null, url, px) {
        value = url?.let { viewModel.image(it, px)?.asImageBitmap() }
    }
    return image
}
