// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.wellbeing

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.core.designsystem.component.BoomArtwork
import com.boomlistener.core.designsystem.component.BoomListArtworkSize
import com.boomlistener.core.designsystem.component.BoomPlaceholder
import com.boomlistener.core.designsystem.component.BoomScreenScaffold
import com.boomlistener.data.library.LibraryData
import com.boomlistener.data.wellbeing.SongStat
import com.boomlistener.data.wellbeing.StatsData

private const val TOP_LIMIT = 50

/**
 * Bem-estar. Por enquanto: as músicas mais ouvidas (regras em
 * core/player/PlayCountTracker). Tempo de escuta e avisos de pausa vêm depois.
 */
@Composable
internal fun WellbeingScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    val stats = remember { StatsData.repository(context) }
    val library = remember { LibraryData.repository(context) }
    val top by remember { stats.topSongs(TOP_LIMIT) }.collectAsStateWithLifecycle(initialValue = emptyList())

    BoomScreenScaffold(
        title = stringResource(R.string.feature_wellbeing_title),
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        if (top.isEmpty()) {
            BoomPlaceholder(
                title = stringResource(R.string.feature_wellbeing_most_played),
                message = stringResource(R.string.feature_wellbeing_most_played_empty),
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(contentPadding = padding) {
                item {
                    Text(
                        text = stringResource(R.string.feature_wellbeing_most_played),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
                    )
                    Text(
                        text = stringResource(R.string.feature_wellbeing_most_played_rule),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    )
                }
                itemsIndexed(top, key = { _, stat -> stat.uri }) { index, stat ->
                    TopSongRow(rank = index + 1, stat = stat) { uri, px ->
                        library.loadArtwork(uri, px)?.asImageBitmap()
                    }
                }
            }
        }
    }
}

@Composable
private fun TopSongRow(rank: Int, stat: SongStat, loadArtwork: suspend (String, Int) -> ImageBitmap?) {
    val px = with(LocalDensity.current) { BoomListArtworkSize.roundToPx() }
    val artwork by produceState<ImageBitmap?>(initialValue = null, stat.uri) { value = loadArtwork(stat.uri, px) }
    ListItem(
        leadingContent = {
            androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(
                    text = rank.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(32.dp),
                )
                BoomArtwork(image = artwork, fallbackText = stat.title)
            }
        },
        headlineContent = { Text(stat.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                text = listOfNotNull(stat.artist, pluralStringResource(R.plurals.feature_wellbeing_plays, stat.plays, stat.plays))
                    .joinToString(" • "),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}
