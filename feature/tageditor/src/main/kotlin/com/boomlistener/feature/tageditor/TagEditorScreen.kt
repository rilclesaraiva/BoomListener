// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.tageditor

import android.Manifest
import androidx.annotation.StringRes
import android.app.Activity
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.core.designsystem.component.BoomArtwork
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.designsystem.component.BoomLoading
import com.boomlistener.core.designsystem.component.BoomPlaceholder
import com.boomlistener.core.designsystem.component.BoomScreenScaffold
import com.boomlistener.core.designsystem.theme.boomEffectsSpec
import com.boomlistener.core.designsystem.theme.boomSpatialSpec
import com.boomlistener.data.deezer.TrackCandidate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val CoverSize = 136.dp

@Composable
internal fun TagEditorScreen(
    viewModel: TagEditorViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var confirmDiscard by remember { mutableStateOf(false) }

    val savedMessage = stringResource(R.string.feature_tageditor_saved)
    val failedMessage = stringResource(R.string.feature_tageditor_save_failed)
    val deniedMessage = stringResource(R.string.feature_tageditor_permission_denied)

    // Android 10+: o próprio sistema pergunta se o app pode alterar o arquivo.
    val writeRequest = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) viewModel.save()
        else scope.launch { snackbar.showSnackbar(deniedMessage) }
    }
    // Android 8 e 9: permissão de gravar no armazenamento.
    val storagePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.save() else scope.launch { snackbar.showSnackbar(deniedMessage) }
    }
    // Capa nova pelo seletor de fotos do Android: não precisa de permissão de fotos.
    val pickCover = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bytes = withContext(Dispatchers.IO) {
                    runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
                }
                if (bytes != null) viewModel.setCover(bytes)
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is TagEditorEvent.AskWritePermission ->
                    writeRequest.launch(IntentSenderRequest.Builder(event.request).build())
                TagEditorEvent.AskStoragePermission ->
                    storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                TagEditorEvent.Saved -> snackbar.showSnackbar(savedMessage)
                TagEditorEvent.SaveFailed -> snackbar.showSnackbar(failedMessage)
            }
        }
    }

    // Com alterações não salvas, "voltar" pergunta antes de descartar.
    val leave: () -> Unit = {
        if (state.hasChanges) {
            confirmDiscard = true
        } else {
            onBack()
        }
    }
    BackHandler(enabled = state.hasChanges, onBack = leave)

    BoomScreenScaffold(
        title = stringResource(R.string.feature_tageditor_title),
        onBack = leave,
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
        actions = {
            if (!state.loading && !state.loadFailed) {
                AnimatedContent(
                    targetState = state.saving,
                    transitionSpec = { fadeIn(boomEffectsSpec()) togetherWith fadeOut(boomEffectsSpec()) },
                    label = "save",
                ) { saving ->
                    if (saving) {
                        Text(
                            text = stringResource(R.string.feature_tageditor_saving),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    } else {
                        FilledTonalButton(
                            onClick = viewModel::save,
                            enabled = state.hasChanges,
                            modifier = Modifier.padding(end = 8.dp),
                        ) {
                            Text(stringResource(R.string.feature_tageditor_save))
                        }
                    }
                }
            }
        },
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when {
            state.loading -> BoomLoading(contentModifier)
            state.loadFailed -> BoomPlaceholder(
                title = stringResource(R.string.feature_tageditor_load_failed_title),
                message = stringResource(R.string.feature_tageditor_load_failed_message),
                modifier = contentModifier,
                actions = {
                    TextButton(onClick = viewModel::load) { Text(stringResource(R.string.feature_tageditor_retry)) }
                },
            )
            else -> EditorForm(
                state = state,
                viewModel = viewModel,
                onPickCover = {
                    pickCover.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = contentModifier,
            )
        }
    }

    val choices = state.autoTag as? AutoTagState.Choosing
    if (choices != null) {
        CandidateSheet(
            candidates = choices.candidates,
            viewModel = viewModel,
            onChoose = viewModel::choose,
            onDismiss = viewModel::dismissChoices,
        )
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.feature_tageditor_discard_title)) },
            text = { Text(stringResource(R.string.feature_tageditor_discard_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    onBack()
                }) { Text(stringResource(R.string.feature_tageditor_discard)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) {
                    Text(stringResource(R.string.feature_tageditor_keep_editing))
                }
            },
        )
    }
}

@Composable
private fun EditorForm(
    state: TagEditorUiState,
    viewModel: TagEditorViewModel,
    onPickCover: () -> Unit,
    modifier: Modifier,
) {
    val tags = state.tags
    val original = state.original
    Column(
        modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CoverRow(state = state, onPickCover = onPickCover, onRemoveCover = viewModel::removeCover)
        AutoTagCard(state = state, viewModel = viewModel)

        TagField(R.string.feature_tageditor_field_title, tags.title, original.title) { value -> viewModel.edit { it.copy(title = value) } }
        TagField(R.string.feature_tageditor_field_artist, tags.artist, original.artist) { value -> viewModel.edit { it.copy(artist = value) } }
        TagField(R.string.feature_tageditor_field_album, tags.album, original.album) { value -> viewModel.edit { it.copy(album = value) } }
        TagField(R.string.feature_tageditor_field_album_artist, tags.albumArtist, original.albumArtist) { value ->
            viewModel.edit { it.copy(albumArtist = value) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TagField(R.string.feature_tageditor_field_year, tags.year, original.year, number = true, modifier = Modifier.weight(1f)) { value ->
                viewModel.edit { it.copy(year = value.filter(Char::isDigit).take(4)) }
            }
            TagField(R.string.feature_tageditor_field_track, tags.trackNumber, original.trackNumber, number = true, modifier = Modifier.weight(1f)) { value ->
                viewModel.edit { it.copy(trackNumber = value.filter(Char::isDigit).take(3)) }
            }
            TagField(R.string.feature_tageditor_field_disc, tags.discNumber, original.discNumber, number = true, modifier = Modifier.weight(1f)) { value ->
                viewModel.edit { it.copy(discNumber = value.filter(Char::isDigit).take(2)) }
            }
        }
        TagField(R.string.feature_tageditor_field_genre, tags.genre, original.genre) { value -> viewModel.edit { it.copy(genre = value) } }
        TagField(R.string.feature_tageditor_field_composer, tags.composer, original.composer) { value -> viewModel.edit { it.copy(composer = value) } }
        TagField(
            label = R.string.feature_tageditor_field_lyrics,
            value = tags.lyrics,
            originalValue = original.lyrics,
            singleLine = false,
            hint = stringResource(R.string.feature_tageditor_field_lyrics_hint),
        ) { value -> viewModel.edit { it.copy(lyrics = value) } }

        AnimatedVisibility(
            visible = state.hasChanges,
            enter = expandVertically(boomSpatialSpec()) + fadeIn(boomEffectsSpec()),
            exit = shrinkVertically(boomSpatialSpec()) + fadeOut(boomEffectsSpec()),
        ) {
            TextButton(onClick = viewModel::resetAll) { Text(stringResource(R.string.feature_tageditor_reset)) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CoverRow(state: TagEditorUiState, onPickCover: () -> Unit, onRemoveCover: () -> Unit) {
    val sizePx = with(LocalDensity.current) { CoverSize.roundToPx() }
    val coverBytes = state.shownCover
    val cover by produceState<ImageBitmap?>(initialValue = null, coverBytes) {
        value = coverBytes?.let { bytes -> withContext(Dispatchers.Default) { decodeSampled(bytes, sizePx) } }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        BoomArtwork(
            image = cover,
            fallbackText = state.tags.title,
            size = CoverSize,
            modifier = Modifier.clickable(onClick = onPickCover),
        )
        Spacer(Modifier.width(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.feature_tageditor_cover),
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedButton(onClick = onPickCover) {
                Icon(BoomIcons.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.feature_tageditor_cover_change))
            }
            AnimatedVisibility(
                visible = coverBytes != null,
                enter = fadeIn(boomEffectsSpec()),
                exit = fadeOut(boomEffectsSpec()),
            ) {
                TextButton(onClick = onRemoveCover) { Text(stringResource(R.string.feature_tageditor_cover_remove)) }
            }
        }
    }
}

/** Cartão das tags automáticas: buscar, ver o que foi preenchido, trocar ou desfazer. */
@Composable
private fun AutoTagCard(state: TagEditorUiState, viewModel: TagEditorViewModel) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(BoomIcons.AutoFix, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.feature_tageditor_auto_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            AnimatedContent(
                targetState = state.autoTag,
                contentKey = { it::class },
                transitionSpec = { fadeIn(boomEffectsSpec()) togetherWith fadeOut(boomEffectsSpec()) },
                label = "autoTag",
            ) { auto ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (auto) {
                        AutoTagState.Searching -> {
                            Text(stringResource(R.string.feature_tageditor_auto_searching), style = MaterialTheme.typography.bodyMedium)
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        is AutoTagState.Applied -> {
                            Text(
                                text = stringResource(
                                    R.string.feature_tageditor_auto_applied,
                                    listOf(auto.chosen.album, auto.chosen.artist).filter { it.isNotBlank() }.joinToString(" · "),
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (auto.others.isNotEmpty()) {
                                    OutlinedButton(onClick = viewModel::showOtherMatches) {
                                        Text(stringResource(R.string.feature_tageditor_auto_other))
                                    }
                                }
                                TextButton(onClick = viewModel::undoAutoTag) {
                                    Text(stringResource(R.string.feature_tageditor_auto_undo))
                                }
                            }
                        }
                        else -> {
                            Text(
                                text = stringResource(
                                    when (auto) {
                                        AutoTagState.NotFound -> R.string.feature_tageditor_auto_not_found
                                        AutoTagState.Failed -> R.string.feature_tageditor_auto_failed
                                        else -> R.string.feature_tageditor_auto_message
                                    },
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Button(onClick = viewModel::autoTag) {
                                Text(stringResource(R.string.feature_tageditor_auto_search))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TagField(
    @StringRes label: Int,
    value: String,
    originalValue: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    number: Boolean = false,
    singleLine: Boolean = true,
    hint: String? = null,
    onValueChange: (String) -> Unit,
) {
    val changed = value != originalValue
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(label)) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 4,
        maxLines = if (singleLine) 1 else 12,
        placeholder = if (hint != null) {
            { Text(hint) }
        } else {
            null
        },
        // Mostra o que mudou em relação ao arquivo (à mão ou pelas tags automáticas).
        supportingText = if (changed) {
            { Text(stringResource(R.string.feature_tageditor_changed), color = MaterialTheme.colorScheme.primary) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (number) KeyboardType.Number else KeyboardType.Text,
            capitalization = if (number) KeyboardCapitalization.None else KeyboardCapitalization.Sentences,
        ),
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CandidateSheet(
    candidates: List<TrackCandidate>,
    viewModel: TagEditorViewModel,
    onChoose: (TrackCandidate) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text(
            text = stringResource(R.string.feature_tageditor_auto_choose_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        LazyColumn(Modifier.padding(bottom = 24.dp)) {
            items(candidates, key = { it.trackId }) { candidate ->
                CandidateRow(candidate, viewModel, onClick = { onChoose(candidate) })
            }
        }
    }
}

@Composable
private fun CandidateRow(candidate: TrackCandidate, viewModel: TagEditorViewModel, onClick: () -> Unit) {
    val sizePx = with(LocalDensity.current) { 56.dp.roundToPx() }
    val cover by produceState<ImageBitmap?>(initialValue = null, candidate.coverUrl) {
        value = candidate.coverUrl?.let { viewModel.remoteCover(it, sizePx)?.asImageBitmap() }
    }
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        leadingContent = { BoomArtwork(image = cover, fallbackText = candidate.title) },
        headlineContent = { Text(candidate.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                text = listOf(candidate.artist, candidate.album).filter { it.isNotBlank() }.joinToString(" · "),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            Text(
                text = stringResource(R.string.feature_tageditor_auto_match, (candidate.confidence * 100).toInt()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        },
    )
}

/** Decodifica a capa já reduzida para ~[sizePx] (não carrega a imagem inteira na memória). */
private fun decodeSampled(bytes: ByteArray, sizePx: Int): ImageBitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= sizePx && bounds.outHeight / (sample * 2) >= sizePx) sample *= 2
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
        ?.asImageBitmap()
}
