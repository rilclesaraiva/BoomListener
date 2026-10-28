// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import com.boomlistener.core.designsystem.component.BoomModalSheet
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.text.style.TextOverflow
import com.boomlistener.core.designsystem.component.BoomSearchField
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.designsystem.theme.boomEffectsSpec
import com.boomlistener.data.library.PlaylistData
import kotlinx.coroutines.launch

/**
 * Folha "Adicionar à playlist": cada playlist mostra se já tem a música;
 * tocar adiciona ou tira (dá para desfazer na hora). Também cria uma nova.
 * Pública porque o :app também usa (menu do player).
 */
@Composable
fun AddToPlaylistSheet(trackUris: List<String>, onDismiss: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val repository = remember { PlaylistData.repository(context) }
    val playlists by repository.playlists.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    var creating by remember { mutableStateOf(false) }
    val effects = boomEffectsSpec<Float>()

    BoomModalSheet(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.feature_library_add_to_playlist),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        LazyColumn(Modifier.weight(1f, fill = false)) {
            item(key = "new") {
                ListItem(
                    modifier = Modifier.clickable { creating = true },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = { Icon(BoomIcons.Add, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.feature_library_new_playlist)) },
                )
            }
            items(playlists, key = { it.id }) { playlist ->
                val inside = trackUris.isNotEmpty() && playlist.trackUris.containsAll(trackUris)
                ListItem(
                    modifier = Modifier.clickable {
                        scope.launch {
                            if (inside) repository.removeTracks(playlist.id, trackUris)
                            else repository.addTracks(playlist.id, trackUris)
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = {
                        AnimatedContent(
                            targetState = inside,
                            transitionSpec = { fadeIn(effects) togetherWith fadeOut(effects) },
                            label = "inside",
                        ) { added ->
                            Icon(
                                if (added) BoomIcons.Check else BoomIcons.Playlist,
                                contentDescription = null,
                                tint = if (added) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    headlineContent = { Text(playlist.name) },
                    supportingContent = {
                        Text(
                            pluralStringResource(
                                R.plurals.feature_library_count,
                                playlist.trackUris.size,
                                playlist.trackUris.size,
                            ),
                        )
                    },
                    trailingContent = { Checkbox(checked = inside, onCheckedChange = null) },
                )
            }
        }
        Button(
            onClick = {
                // Desce o painel e só então fecha.
                dismiss()
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        ) { Text(stringResource(R.string.feature_library_done)) }
    }

    if (creating) {
        PlaylistNameDialog(
            title = stringResource(R.string.feature_library_new_playlist),
            initialName = stringResource(R.string.feature_library_playlist_default_name, playlists.size + 1),
            confirmLabel = stringResource(R.string.feature_library_create),
            onConfirm = { name ->
                creating = false
                scope.launch { repository.create(name, trackUris) }
            },
            onDismiss = { creating = false },
        )
    }
}

/**
 * "Adicionar músicas" dentro de uma playlist: todas as músicas do aparelho,
 * com busca; marcar adiciona, desmarcar tira.
 */
@Composable
internal fun AddSongsSheet(viewModel: CollectionDetailViewModel, onDismiss: () -> Unit) {
    val all by viewModel.libraryTracks.collectAsStateWithLifecycle()
    val inside by viewModel.playlistUris.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val shown = remember(all, query) { filterTracks(all, query) }
    val scope = rememberCoroutineScope()

    BoomModalSheet(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.feature_library_add_songs),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        BoomSearchField(
            query = query,
            onQueryChange = { query = it },
            placeholder = stringResource(R.string.feature_library_add_songs_search),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        )
        LazyColumn(Modifier.weight(1f, fill = false)) {
            items(shown, key = { it.uri }) { track ->
                val checked = track.uri in inside
                ListItem(
                    modifier = Modifier.clickable { viewModel.toggleInPlaylist(track.uri) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = {
                        Text(
                            track.artist ?: stringResource(R.string.feature_library_unknown_artist),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    trailingContent = { Checkbox(checked = checked, onCheckedChange = null) },
                )
            }
        }
        Button(
            onClick = {
                // Desce o painel e só então fecha.
                dismiss()
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        ) { Text(stringResource(R.string.feature_library_done)) }
    }
}

/** Diálogo com o nome da playlist (criar ou renomear). */
@Composable
internal fun PlaylistNameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var field by remember { mutableStateOf(TextFieldValue(initialName, TextRange(0, initialName.length))) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = field,
                onValueChange = { field = it },
                singleLine = true,
                label = { Text(stringResource(R.string.feature_library_playlist_name)) },
                modifier = Modifier.focusRequester(focus),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(field.text) }, enabled = field.text.isNotBlank()) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_library_cancel)) }
        },
    )
}

/**
 * "Nova playlist" fora da Biblioteca (barra lateral em telas largas): pede o
 * nome, cria e devolve o id em [onCreated] para abrir a playlist.
 */
@Composable
fun NewPlaylistDialog(onCreated: (id: Long) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val repository = remember { PlaylistData.repository(context) }
    val playlists by repository.playlists.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    PlaylistNameDialog(
        title = stringResource(R.string.feature_library_new_playlist),
        initialName = stringResource(R.string.feature_library_playlist_default_name, playlists.size + 1),
        confirmLabel = stringResource(R.string.feature_library_create),
        onConfirm = { name ->
            scope.launch {
                val id = repository.create(name)
                onDismiss()
                onCreated(id)
            }
        },
        onDismiss = onDismiss,
    )
}
