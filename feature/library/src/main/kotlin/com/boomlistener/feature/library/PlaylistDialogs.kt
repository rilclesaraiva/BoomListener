// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import com.boomlistener.core.designsystem.component.BoomModalSheet
import androidx.compose.material3.Button
import com.boomlistener.core.designsystem.component.BoomMenuPageScope
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.Alignment
import androidx.compose.material3.FilledIconButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
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
 * Página "Adicionar à playlist" (dentro do painel do menu): cada playlist mostra se já tem a
 * música; tocar adiciona ou remove (dá para desfazer na hora). Também cria uma nova, ali mesmo.
 * Pública porque o `:app` a entrega aos menus de todas as features (`LocalAddToPlaylistPage`).
 */
@Composable
fun BoomMenuPageScope.AddToPlaylistPage(trackUris: List<String>) {
    val context = LocalContext.current.applicationContext
    val repository = remember { PlaylistData.repository(context) }
    val playlists by repository.playlists.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    var creating by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    val defaultName = stringResource(R.string.feature_library_playlist_default_name, playlists.size + 1)
    val effects = boomEffectsSpec<Float>()

    Column(Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.feature_library_add_to_playlist),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        LazyColumn(Modifier.weight(1f, fill = false)) {
            item(key = "new") {
                if (creating) {
                    val create = {
                        val name = newName.trim().ifEmpty { defaultName }
                        creating = false
                        scope.launch { repository.create(name, trackUris) }
                        Unit
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                    ) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            singleLine = true,
                            placeholder = { Text(defaultName) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { create() }),
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        FilledIconButton(onClick = { create() }) {
                            Icon(BoomIcons.Check, contentDescription = stringResource(R.string.feature_library_create))
                        }
                    }
                } else {
                    ListItem(
                        modifier = Modifier.clickable { newName = ""; creating = true },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = { Icon(BoomIcons.Add, contentDescription = null) },
                        headlineContent = { Text(stringResource(R.string.feature_library_new_playlist)) },
                    )
                }
            }
            items(playlists.filterNot { it.locked }, key = { it.id }) { playlist ->
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
            onClick = { dismiss() },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        ) { Text(stringResource(R.string.feature_library_done)) }
    }
}

/**
 * Página "Adicionar músicas" dentro de uma playlist (no painel do menu): todas as músicas do aparelho,
 * com busca; marcar adiciona, desmarcar tira.
 */
@Composable
internal fun BoomMenuPageScope.AddSongsPage(viewModel: CollectionDetailViewModel) =
    AddSongsContent(viewModel, onDone = { dismiss() })

/** O botão "+" do topo da playlist (que não é um menu) abre a mesma lista num painel próprio. */
@Composable
internal fun AddSongsSheet(viewModel: CollectionDetailViewModel, onDismiss: () -> Unit) {
    BoomModalSheet(onDismissRequest = onDismiss) {
        AddSongsContent(viewModel, onDone = { dismiss() })
    }
}

@Composable
private fun AddSongsContent(viewModel: CollectionDetailViewModel, onDone: () -> Unit) {
    val all by viewModel.libraryTracks.collectAsStateWithLifecycle()
    val inside by viewModel.playlistUris.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val shown = remember(all, query) { filterTracks(all, query) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxWidth()) {
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
            onClick = onDone,
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
