// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boomlistener.core.model.Playlist
import com.boomlistener.core.model.Track
import com.boomlistener.data.library.LibraryRepository
import com.boomlistener.data.library.PlaylistRepository
import com.boomlistener.data.library.TrackGroup
import com.boomlistener.data.library.groupByAlbum
import com.boomlistener.data.library.groupByArtist
import com.boomlistener.data.library.groupByGenre
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** As seções do carrossel da Biblioteca, na ordem em que aparecem. */
internal enum class CollectionKind { Genres, Artists, Playlists, Albums }

/**
 * "Ver todos" do Início: qual pílula abrir quando a aba Biblioteca aparecer.
 * Fica fora do ViewModel porque o Início e a Biblioteca são telas diferentes.
 */
internal object CollectionsRequest {
    val kind = MutableStateFlow<CollectionKind?>(null)
}

/** Uma playlist com as músicas que ainda estão no aparelho. */
internal data class PlaylistSummary(val playlist: Playlist, val tracks: List<Track>)

internal sealed interface CollectionsUiState {
    data object Loading : CollectionsUiState
    data class Ready(
        val selected: CollectionKind,
        val genres: List<TrackGroup>,
        val artists: List<TrackGroup>,
        val albums: List<TrackGroup>,
        val playlists: List<PlaylistSummary>,
    ) : CollectionsUiState
}

/** Aba Biblioteca: gêneros, artistas, playlists e álbuns. */
internal class CollectionsViewModel(
    private val library: LibraryRepository,
    private val playlistRepository: PlaylistRepository,
) : ViewModel() {

    private val tracks = MutableStateFlow<List<Track>?>(null)
    private val selected = MutableStateFlow(CollectionKind.Genres)

    val state: StateFlow<CollectionsUiState> =
        combine(tracks, playlistRepository.playlists, selected) { all, playlists, kind ->
            if (all == null) {
                CollectionsUiState.Loading
            } else {
                CollectionsUiState.Ready(
                    selected = kind,
                    genres = groupByGenre(all),
                    artists = groupByArtist(all),
                    albums = groupByAlbum(all),
                    playlists = playlists.map { summarize(it, all) },
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CollectionsUiState.Loading)

    init {
        load()
        // Tags editadas: gêneros, artistas e álbuns podem ter mudado.
        viewModelScope.launch { library.version.drop(1).collect { load() } }
    }

    fun load() {
        viewModelScope.launch { tracks.value = library.getTracks() }
    }

    fun select(kind: CollectionKind) {
        selected.value = kind
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch { playlistRepository.create(name) }
    }

    suspend fun artwork(uri: String, sizePx: Int): Bitmap? = library.loadArtwork(uri, sizePx)
}

/** As músicas da playlist, na ordem dela, sem as que saíram do aparelho. */
internal fun summarize(playlist: Playlist, library: List<Track>): PlaylistSummary {
    val byUri = library.associateBy { it.uri }
    return PlaylistSummary(playlist, playlist.trackUris.mapNotNull { byUri[it] })
}
