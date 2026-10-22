// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.boomlistener.core.designsystem.component.BoomPageTitle
import com.boomlistener.core.designsystem.component.BoomScreenScaffold
import com.boomlistener.core.designsystem.component.BoomSettingsItem
import com.boomlistener.core.designsystem.component.BoomSettingsSection
import com.boomlistener.core.designsystem.theme.boomEffectsSpec
import com.boomlistener.core.designsystem.theme.boomSpatialSpec
import com.boomlistener.data.library.LibraryData
import com.boomlistener.data.library.LibraryFilter
import kotlin.math.roundToInt

/** Passo da barra de duração mínima, em segundos. */
private const val DURATION_STEP = 5

@Composable
internal fun LibrarySettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    val viewModel: LibrarySettingsViewModel = viewModel {
        LibrarySettingsViewModel(LibraryData.repository(context))
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val filter = state.filter

    BoomScreenScaffold(
        title = stringResource(R.string.feature_settings_library),
        onBack = onBack,
        modifier = modifier,
        // Sem barra presa na tela: o título é o topo da página.
        topBar = {},
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            item(key = "pageTitle") {
                BoomPageTitle(stringResource(R.string.feature_settings_library), hasBack = true, large = true)
            }
            item(key = "duration") {
                BoomSettingsSection(title = stringResource(R.string.feature_settings_library_duration)) {
                    DurationSlider(
                        savedSeconds = filter.minDurationSeconds,
                        onCommit = viewModel::setMinDuration,
                    )
                }
            }
            item(key = "source") {
                BoomSettingsSection(title = stringResource(R.string.feature_settings_library_folders)) {
                    BoomSettingsItem(
                        icon = null,
                        title = stringResource(R.string.feature_settings_library_all),
                        subtitle = stringResource(R.string.feature_settings_library_all_summary),
                        onClick = viewModel::useAllFolders,
                        trailing = { RadioButton(selected = !filter.onlyChosenFolders, onClick = viewModel::useAllFolders) },
                    )
                    BoomSettingsItem(
                        icon = null,
                        title = stringResource(R.string.feature_settings_library_chosen),
                        subtitle = stringResource(R.string.feature_settings_library_chosen_summary),
                        onClick = viewModel::useChosenFolders,
                        trailing = { RadioButton(selected = filter.onlyChosenFolders, onClick = viewModel::useChosenFolders) },
                    )
                }
            }
            item(key = "folders") {
                FolderList(state, viewModel)
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

/**
 * A barra mexe na hora, mas a biblioteca só recarrega quando o dedo solta
 * (senão recarregaria a cada passo).
 */
@Composable
private fun DurationSlider(savedSeconds: Int, onCommit: (Int) -> Unit) {
    var dragging by remember { mutableFloatStateOf(-1f) }
    val value = if (dragging >= 0f) dragging else savedSeconds.toFloat()
    val seconds = value.roundToInt()
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = if (seconds == 0) {
                    stringResource(R.string.feature_settings_library_duration_off)
                } else {
                    pluralStringResource(R.plurals.feature_settings_library_duration_on, seconds, seconds)
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.feature_settings_library_duration_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Slider(
                value = value,
                onValueChange = { dragging = it },
                onValueChangeFinished = {
                    if (dragging >= 0f) onCommit(dragging.roundToInt())
                    dragging = -1f
                },
                valueRange = 0f..LibraryFilter.MAX_MIN_DURATION_SECONDS.toFloat(),
                steps = LibraryFilter.MAX_MIN_DURATION_SECONDS / DURATION_STEP - 1,
            )
        }
    }
}

/** As pastas com música, com caixinha; só aparece no modo "pastas escolhidas". */
@Composable
private fun FolderList(state: LibrarySettingsUiState, viewModel: LibrarySettingsViewModel) {
    AnimatedVisibility(
        visible = state.filter.onlyChosenFolders,
        enter = expandVertically(boomSpatialSpec()) + fadeIn(boomEffectsSpec()),
        exit = shrinkVertically(boomSpatialSpec()) + fadeOut(boomEffectsSpec()),
    ) {
        val folders = state.folders
        BoomSettingsSection(title = stringResource(R.string.feature_settings_library_pick)) {
            when {
                folders == null -> Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        CircularProgressIndicator()
                    }
                }
                folders.isEmpty() -> BoomSettingsItem(
                    icon = null,
                    title = stringResource(R.string.feature_settings_library_no_folders),
                )
                else -> {
                    if (state.filter.folders.none { chosen -> folders.any { it.path == chosen || it.path.startsWith("$chosen/") } }) {
                        BoomSettingsItem(
                            icon = null,
                            title = stringResource(R.string.feature_settings_library_none_chosen),
                        )
                    }
                    folders.forEach { folder ->
                        val checked = folder.path in state.filter.folders
                        BoomSettingsItem(
                            icon = null,
                            title = folder.name,
                            subtitle = folder.location + " · " +
                                pluralStringResource(R.plurals.feature_settings_library_songs, folder.songs, folder.songs),
                            onClick = { viewModel.toggleFolder(folder.path) },
                            trailing = { Checkbox(checked = checked, onCheckedChange = { viewModel.toggleFolder(folder.path) }) },
                        )
                    }
                }
            }
        }
    }
}
