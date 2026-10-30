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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class LibraryViewModel(
    private val repository: LibraryRepository,
    private val player: PlaybackController,
    /** Foto do artista no Deezer pelo nome (null nos testes ou sem achar). */
    private val artistPhoto: suspend (name: String, sizePx: Int) -> Bitmap? = { _, _ -> null },
) : ViewModel() {

    // null = ainda não carregou.
    private val allTracks = MutableStateFlow<List<Track>?>(null)
    private val query = MutableStateFlow("")

    // Recentes, álbuns e artistas: só recalcula quando as músicas mudam (não a cada letra da busca).
    private val home = allTracks.map { tracks -> tracks?.let { buildHomeSections(it) } }

    val uiState: StateFlow<LibraryUiState> =
        combine(allTracks, query, repository.version, home) { tracks, currentQuery, version, sections ->
            if (tracks == null) {
                LibraryUiState.Loading
            } else {
                LibraryUiState.Success(
                    tracks = filterTracks(tracks, currentQuery),
                    totalCount = tracks.size,
                    query = currentQuery,
                    version = version,
                    // O topo fica também durante a busca: nada pula enquanto se digita.
                    home = sections?.takeIf { !it.isEmpty },
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

    /** "Tocar": a lista da tela, do começo. */
    fun playAll() {
        val visible = (uiState.value as? LibraryUiState.Success)?.tracks ?: return
        if (visible.isNotEmpty()) player.play(visible, 0)
    }

    /** Toca as adicionadas recentemente, a partir da de posição [index]. */
    fun playRecent(index: Int) {
        val recent = (uiState.value as? LibraryUiState.Success)?.home?.recent ?: return
        if (index in recent.indices) player.play(recent, index)
    }

    /** "Tocar aleatório": embaralha o que está na tela (toda a lista ou a busca). */
    /** Toca uma lista (ex.: um álbum pelo menu), do começo. */
    fun playTracks(tracks: List<Track>) {
        if (tracks.isNotEmpty()) player.play(tracks, 0)
    }

    fun shuffleTracks(tracks: List<Track>) {
        if (tracks.isNotEmpty()) player.playShuffled(tracks)
    }

    fun shuffleAll() {
        val visible = (uiState.value as? LibraryUiState.Success)?.tracks ?: return
        if (visible.isNotEmpty()) player.playShuffled(visible)
    }

    /** Link para compartilhar: a música no YouTube Music, ou a busca por ela. */
    suspend fun shareUrl(track: Track): String =
        ExternalLinks.youtubeMusicSearch(track.title, track.artist)

    suspend fun artwork(track: Track, sizePx: Int): Bitmap? =
        repository.loadArtwork(track.uri, sizePx)

    /** Foto do artista: a do Deezer pelo nome (null sem internet ou sem achar; aí vale a capa). */
    suspend fun artistPhoto(name: String, sizePx: Int): Bitmap? =
        if (name.isBlank()) null else runCatching { artistPhoto.invoke(name, sizePx) }.getOrNull()
}
