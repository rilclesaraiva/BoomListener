// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boomlistener.core.model.Track
import com.boomlistener.core.player.PlaybackController
import com.boomlistener.data.library.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import com.boomlistener.core.common.ExternalLinks
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class LibraryViewModel(
    private val repository: LibraryRepository,
    private val player: PlaybackController,
    /** Link do YouTube Music de uma música do aparelho (null se não achar). */
    private val songLink: suspend (title: String, artist: String?, durationMs: Long) -> String? = { _, _, _ -> null },
) : ViewModel() {

    // null = ainda não carregou.
    private val allTracks = MutableStateFlow<List<Track>?>(null)
    private val query = MutableStateFlow("")

    val uiState: StateFlow<LibraryUiState> =
        combine(allTracks, query, repository.version) { tracks, currentQuery, version ->
            if (tracks == null) {
                LibraryUiState.Loading
            } else {
                LibraryUiState.Success(
                    tracks = filterTracks(tracks, currentQuery),
                    totalCount = tracks.size,
                    query = currentQuery,
                    version = version,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState.Loading)

    init {
        // Tags editadas: relê a lista (títulos e capas novos).
        viewModelScope.launch {
            repository.version.drop(1).collect { if (allTracks.value != null) load() }
        }
    }

    /** Lê (ou relê) as músicas do aparelho. Chamado quando a permissão existe. */
    fun load() {
        viewModelScope.launch { allTracks.value = repository.getTracks() }
    }

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }

    /**
     * Toca [track] usando como fila a lista que está na tela (se houver busca,
     * só os resultados dela).
     */
    fun play(track: Track) {
        val visible = (uiState.value as? LibraryUiState.Success)?.tracks ?: return
        val index = visible.indexOfFirst { it.id == track.id }.takeIf { it >= 0 } ?: return
        player.play(visible, index)
    }

    /** "Tocar aleatório": embaralha o que está na tela (toda a lista ou a busca). */
    fun shuffleAll() {
        val visible = (uiState.value as? LibraryUiState.Success)?.tracks ?: return
        if (visible.isNotEmpty()) player.playShuffled(visible)
    }

    /** Link para compartilhar: a música no YouTube Music, ou a busca por ela. */
    suspend fun shareUrl(track: Track): String =
        runCatching { songLink(track.title, track.artist, track.durationMs) }.getOrNull()
            ?: ExternalLinks.youtubeMusicSearch(track.title, track.artist)

    suspend fun artwork(track: Track, sizePx: Int): Bitmap? =
        repository.loadArtwork(track.uri, sizePx)
}
