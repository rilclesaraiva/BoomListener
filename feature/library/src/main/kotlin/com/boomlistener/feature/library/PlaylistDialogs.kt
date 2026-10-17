// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Folha "Adicionar à playlist": escolhe uma playlist (ou cria uma nova) e
 * acrescenta [trackUris]. Mostra um ✓ na escolhida e fecha sozinha.
 * Pública porque o :app também usa (menu do player).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(trackUris: List<String>, onDismiss: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val repository = remember { PlaylistData.repository(context) }
    val playlists by repository.playlists.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var creating by remember { mutableStateOf(false) }
    var addedTo by remember { mutableStateOf<Long?>(null) }
    val effects = boomEffectsSpec<Float>()

    // Depois de adicionar, deixa o ✓ aparecer e fecha com a animação da folha.
    LaunchedEffect(addedTo) {
        if (addedTo != null) {
            delay(700)
            sheetState.hide()
            onDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text(
            text = stringResource(R.string.feature_library_add_to_playlist),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        LazyColumn(Modifier.padding(bottom = 24.dp)) {
            item(key = "new") {
                ListItem(
                    modifier = Modifier.clickable { creating = true },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = { Icon(BoomIcons.Add, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.feature_library_new_playlist)) },
                )
            }
            items(playlists, key = { it.id }) { playlist ->
                ListItem(
                    modifier = Modifier.clickable(enabled = addedTo == null) {
                        scope.launch {
                            repository.addTracks(playlist.id, trackUris)
                            addedTo = playlist.id
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = {
                        AnimatedContent(
                            targetState = addedTo == playlist.id,
                            transitionSpec = { fadeIn(effects) togetherWith fadeOut(effects) },
                            label = "added",
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
                )
            }
        }
    }

    if (creating) {
        PlaylistNameDialog(
            title = stringResource(R.string.feature_library_new_playlist),
            initialName = stringResource(R.string.feature_library_playlist_default_name, playlists.size + 1),
            confirmLabel = stringResource(R.string.feature_library_create),
            onConfirm = { name ->
                creating = false
                scope.launch {
                    val id = repository.create(name, trackUris)
                    addedTo = id
                }
            },
            onDismiss = { creating = false },
        )
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
