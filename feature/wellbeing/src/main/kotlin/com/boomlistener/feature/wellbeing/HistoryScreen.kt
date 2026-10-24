// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.wellbeing

import com.boomlistener.core.designsystem.component.clickableWithMenu
import com.boomlistener.core.common.ExternalLinks
import com.boomlistener.core.common.openUrl
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import com.boomlistener.core.designsystem.component.BoomFastScroller
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.core.designsystem.component.BoomArtwork
import com.boomlistener.core.designsystem.component.BoomListArtworkSize
import com.boomlistener.core.designsystem.component.BoomLoading
import com.boomlistener.core.designsystem.component.BoomMenuAction
import com.boomlistener.core.designsystem.component.BoomMenuHeader
import com.boomlistener.core.designsystem.component.BoomMenuKind
import com.boomlistener.core.designsystem.component.BoomMoreMenu
import com.boomlistener.core.designsystem.component.BoomPlaceholder
import com.boomlistener.core.designsystem.component.BoomScreenScaffold
import com.boomlistener.data.wellbeing.HistoryEntry
import com.boomlistener.data.wellbeing.HistoryPeriod
import com.boomlistener.data.wellbeing.HistorySection
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Histórico de escuta: o que você ouviu hoje, ontem, nesta semana… */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit,
    songMenu: HistorySongMenu,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }
    val hasEntries = (state as? HistoryUiState.Ready)?.sections?.isNotEmpty() == true

    BoomScreenScaffold(
        title = stringResource(R.string.feature_wellbeing_history_title),
        onBack = onBack,
        modifier = modifier,
        actions = {
            if (hasEntries) {
                BoomMoreMenu(
                    actions = listOf(
                        BoomMenuAction(stringResource(R.string.feature_wellbeing_history_clear), kind = BoomMenuKind.Delete) { confirmClear = true },
                    ),
                )
            }
        },
    ) { padding ->
        when (val current = state) {
            HistoryUiState.Loading -> BoomLoading(Modifier.padding(padding))
            is HistoryUiState.Ready -> if (current.sections.isEmpty()) {
                BoomPlaceholder(
                    title = stringResource(R.string.feature_wellbeing_history_empty_title),
                    message = stringResource(R.string.feature_wellbeing_history_empty_message),
                    modifier = Modifier.padding(padding),
                )
            } else {
                val listState = rememberLazyListState()
                // Rótulo de cada item: o período da seção ("Hoje", "Ontem"…).
                val titles = current.sections.map { sectionTitle(it) }
                val labels = remember(current, titles) {
                    current.sections.zip(titles).flatMap { (section, title) -> List(section.entries.size + 1) { title } }
                }
                Box(Modifier.fillMaxSize()) {
                LazyColumn(state = listState, contentPadding = padding) {
                    current.sections.forEach { section ->
                        stickyHeader(key = "${section.period}-${section.month}") { SectionHeader(section) }
                        items(section.entries, key = { "${it.timestampMs}-${it.uri}" }) { entry ->
                            HistoryRow(
                                entry = entry,
                                showDate = section.period == HistoryPeriod.ThisWeek ||
                                    section.period == HistoryPeriod.LastWeek ||
                                    section.period == HistoryPeriod.Earlier,
                                viewModel = viewModel,
                                songMenu = songMenu,
                                onClick = { viewModel.play(entry, section) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
                BoomFastScroller(
                    state = listState,
                    label = { index -> labels.getOrNull(index) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding()),
                )
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.feature_wellbeing_history_clear_title)) },
            text = { Text(stringResource(R.string.feature_wellbeing_history_clear_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    viewModel.clear()
                }) { Text(stringResource(R.string.feature_wellbeing_history_clear)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.feature_wellbeing_cancel)) }
            },
        )
    }
}

@Composable
private fun SectionHeader(section: HistorySection) {
    val title = sectionTitle(section)
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
        )
    }
}

@Composable
private fun sectionTitle(section: HistorySection): String =
    when (section.period) {
        HistoryPeriod.Today -> stringResource(R.string.feature_wellbeing_history_today)
        HistoryPeriod.Yesterday -> stringResource(R.string.feature_wellbeing_history_yesterday)
        HistoryPeriod.ThisWeek -> stringResource(R.string.feature_wellbeing_history_this_week)
        HistoryPeriod.LastWeek -> stringResource(R.string.feature_wellbeing_history_last_week)
        HistoryPeriod.Earlier -> section.month
            ?.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault()))
            ?.replaceFirstChar { it.titlecase(Locale.getDefault()) }
            .orEmpty()
    }

@Composable
private fun HistoryRow(
    entry: HistoryEntry,
    showDate: Boolean,
    viewModel: HistoryViewModel,
    songMenu: HistorySongMenu,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val menuOpen = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val px = with(LocalDensity.current) { BoomListArtworkSize.roundToPx() }
    val artwork by produceState<ImageBitmap?>(initialValue = null, entry.uri) {
        value = viewModel.artwork(entry.uri, px)?.asImageBitmap()
    }
    val time = remember(entry.timestampMs, showDate) {
        val moment = Instant.ofEpochMilli(entry.timestampMs).atZone(ZoneId.systemDefault())
        val clock = moment.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
        if (showDate) {
            moment.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())) + " · " + clock
        } else {
            clock
        }
    }
    ListItem(
        // Segurar abre o mesmo menu dos três pontinhos.
        modifier = modifier.clickableWithMenu(menuOpen, onClick = onClick),
        leadingContent = { BoomArtwork(image = artwork, fallbackText = entry.title) },
        headlineContent = { Text(entry.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                text = listOfNotNull(entry.artist, time).joinToString(" • "),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            BoomMoreMenu(
                actions = listOfNotNull(
                    playNextAction(entry.uri),
                    BoomMenuAction(stringResource(R.string.feature_wellbeing_add_to_playlist), kind = BoomMenuKind.AddToPlaylist) { songMenu.onAddToPlaylist(entry.uri) },
                    favoriteAction(entry.uri),
                    entry.artist?.let { name ->
                        BoomMenuAction(stringResource(R.string.feature_wellbeing_go_to_artist), kind = BoomMenuKind.GoToArtist) { songMenu.onOpenArtist(name) }
                    },
                    BoomMenuAction(stringResource(R.string.feature_wellbeing_edit_tags), kind = BoomMenuKind.EditTags) { songMenu.onEditTags(entry.uri) },
                ),
                header = BoomMenuHeader(entry.title, entry.artist, artwork),
                onYoutubeMusic = { context.openUrl(ExternalLinks.youtubeMusicSearch(entry.title, entry.artist)) },
                openState = menuOpen,
            )
        },
    )
}
