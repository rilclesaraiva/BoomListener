// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.tageditor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.core.designsystem.component.BoomPageTitle
import com.boomlistener.core.designsystem.component.BoomScreenScaffold
import com.boomlistener.core.designsystem.component.BoomSettingsItem
import com.boomlistener.core.designsystem.component.BoomSettingsSection
import com.boomlistener.data.tags.TagGap

@Composable
private fun TagGap.label(): String = stringResource(
    when (this) {
        TagGap.Artist -> R.string.feature_tageditor_audit_gap_artist
        TagGap.Album -> R.string.feature_tageditor_audit_gap_album
        TagGap.Year -> R.string.feature_tageditor_audit_gap_year
        TagGap.Genre -> R.string.feature_tageditor_audit_gap_genre
        TagGap.TrackNumber -> R.string.feature_tageditor_audit_gap_track
        TagGap.AlbumArtist -> R.string.feature_tageditor_audit_gap_album_artist
        TagGap.Composer -> R.string.feature_tageditor_audit_gap_composer
        TagGap.Cover -> R.string.feature_tageditor_audit_gap_cover
        TagGap.Lyrics -> R.string.feature_tageditor_audit_gap_lyrics
    },
)

/**
 * Escaneamento de tags (Ajustes → Biblioteca): lê a biblioteca, mostra quantas
 * músicas não têm cada coisa e leva ao editor em sequência.
 */
@Composable
internal fun TagAuditScreen(
    viewModel: TagAuditViewModel,
    onBack: () -> Unit,
    onEditTracks: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // A lista aberta (guardada pelo nome: sobrevive a girar a tela).
    var openName by rememberSaveable { mutableStateOf<String?>(null) }
    val open = TagGap.entries.firstOrNull { it.name == openName }?.takeIf { it in state.found }

    // Ao voltar do editor, as músicas que foram abertas são lidas de novo.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshEdited()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(enabled = open != null) { openName = null }

    val title = open?.label() ?: stringResource(R.string.feature_tageditor_audit_title)
    BoomScreenScaffold(
        title = title,
        onBack = { if (open != null) openName = null else onBack() },
        modifier = modifier,
        topBar = {},
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            item(key = "pageTitle") { BoomPageTitle(title, hasBack = true, large = true) }
            if (open == null) {
                overview(state, onScan = viewModel::scan, onOpen = { openName = it.name })
            } else {
                val songs = state.found.getValue(open)
                if (songs.size > 1) {
                    item(key = "editAll") {
                        FilledTonalButton(
                            onClick = {
                                viewModel.markEdited(songs.map { it.uri })
                                onEditTracks(songs.map { it.uri })
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        ) { Text(stringResource(R.string.feature_tageditor_audit_edit_all, songs.size)) }
                    }
                }
                items(songs, key = { it.uri }) { song ->
                    ListItem(
                        headlineContent = { Text(song.title, maxLines = 1) },
                        supportingContent = { Text(song.artist.orEmpty(), maxLines = 1) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable {
                            // Abre esta e segue pelas seguintes da lista.
                            val rest = songs.dropWhile { it.uri != song.uri }.map { it.uri }
                            viewModel.markEdited(rest)
                            onEditTracks(rest)
                        },
                    )
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.overview(
    state: TagAuditState,
    onScan: () -> Unit,
    onOpen: (TagGap) -> Unit,
) {
    item(key = "intro") {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                stringResource(R.string.feature_tageditor_audit_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when (state.phase) {
                ScanPhase.Idle -> Button(onClick = onScan, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.feature_tageditor_audit_scan))
                }
                ScanPhase.Scanning -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(
                        progress = { if (state.total == 0) 0f else state.scanned.toFloat() / state.total },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        stringResource(R.string.feature_tageditor_audit_progress, state.scanned, state.total),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                ScanPhase.Done -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        pluralStringResource(R.plurals.feature_tageditor_audit_summary, state.scanned, state.scanned),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    if (state.unreadable > 0) {
                        Text(
                            pluralStringResource(R.plurals.feature_tageditor_audit_unreadable, state.unreadable, state.unreadable),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    FilledTonalButton(onClick = onScan, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.feature_tageditor_audit_rescan))
                    }
                }
            }
        }
    }
    if (state.phase == ScanPhase.Done) {
        if (state.found.isEmpty()) {
            item(key = "allGood") {
                Text(
                    stringResource(R.string.feature_tageditor_audit_all_good),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                )
            }
        } else {
            item(key = "gaps") {
                BoomSettingsSection(title = null) {
                    state.found.forEach { (gap, songs) ->
                        BoomSettingsItem(
                            icon = null,
                            title = gap.label(),
                            onClick = { onOpen(gap) },
                            trailing = { Text(songs.size.toString(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(end = 4.dp), color = MaterialTheme.colorScheme.primary) },
                        )
                    }
                }
            }
        }
    }
}
