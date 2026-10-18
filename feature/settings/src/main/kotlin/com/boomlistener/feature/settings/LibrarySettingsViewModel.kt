// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boomlistener.data.library.LibraryFilter
import com.boomlistener.data.library.LibraryRepository
import com.boomlistener.data.library.MusicFolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** [folders] = null enquanto a lista de pastas ainda está sendo lida. */
internal data class LibrarySettingsUiState(
    val filter: LibraryFilter = LibraryFilter(),
    val folders: List<MusicFolder>? = null,
)

/** Ajustes → Biblioteca: duração mínima e de quais pastas vêm as músicas. */
internal class LibrarySettingsViewModel(
    private val library: LibraryRepository,
) : ViewModel() {

    private val folders = MutableStateFlow<List<MusicFolder>?>(null)

    val state: StateFlow<LibrarySettingsUiState> =
        combine(library.filter, folders) { filter, list -> LibrarySettingsUiState(filter, list) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, LibrarySettingsUiState(library.filter.value))

    init {
        loadFolders()
        // Música nova baixada enquanto a tela está aberta: a pasta aparece.
        viewModelScope.launch { library.version.drop(1).collect { loadFolders() } }
    }

    private fun loadFolders() {
        viewModelScope.launch { folders.value = library.folders() }
    }

    fun setMinDuration(seconds: Int) {
        val clean = seconds.coerceIn(0, LibraryFilter.MAX_MIN_DURATION_SECONDS)
        val current = library.filter.value
        if (current.minDurationSeconds != clean) library.setFilter(current.copy(minDurationSeconds = clean))
    }

    fun useAllFolders() {
        library.setFilter(library.filter.value.copy(onlyChosenFolders = false))
    }

    /**
     * Passa a usar só as pastas escolhidas. Se nenhuma foi escolhida ainda,
     * começa com todas marcadas: a biblioteca não fica vazia de repente, e a
     * pessoa só desmarca o que não quer.
     */
    fun useChosenFolders() {
        val current = library.filter.value
        val known = folders.value.orEmpty().map { it.path }.toSet()
        val chosen = current.folders.ifEmpty { known }
        library.setFilter(current.copy(onlyChosenFolders = true, folders = chosen))
    }

    fun toggleFolder(path: String) {
        val current = library.filter.value
        val chosen = if (path in current.folders) current.folders - path else current.folders + path
        library.setFilter(current.copy(onlyChosenFolders = true, folders = chosen))
    }
}
