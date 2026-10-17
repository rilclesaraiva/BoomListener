// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.artist

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boomlistener.core.model.AlbumDetail
import com.boomlistener.core.model.Track
import com.boomlistener.core.player.PlaybackController
import com.boomlistener.data.deezer.AlbumResult
import com.boomlistener.data.deezer.DeezerRepository
import com.boomlistener.data.library.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal sealed interface AlbumUiState {
    data object Loading : AlbumUiState
    data object Failed : AlbumUiState

    /** [local] tem, para cada música do álbum, a sua versão no aparelho (ou null). */
    data class Ready(val album: AlbumDetail, val local: List<Track?>) : AlbumUiState {
        val ownedCount: Int get() = local.count { it != null }
    }
}

internal class AlbumViewModel(
    private val albumId: Long,
    private val artistName: String,
    private val deezer: DeezerRepository,
    private val library: LibraryRepository,
    private val player: PlaybackController,
) : ViewModel() {

    private val _state = MutableStateFlow<AlbumUiState>(AlbumUiState.Loading)
    val state: StateFlow<AlbumUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = AlbumUiState.Loading
        viewModelScope.launch {
            _state.value = when (val result = deezer.album(albumId)) {
                is AlbumResult.Found -> {
                    val mine = tracksOfArtist(library.getTracks(), artistName.ifBlank { result.album.artistName })
                    AlbumUiState.Ready(result.album, matchAlbumTracks(result.album.tracks, mine))
                }
                AlbumResult.Failed -> AlbumUiState.Failed
            }
        }
    }

    /** Toca as músicas do álbum que você tem, começando pela escolhida. */
    fun playOwned(fromAlbumIndex: Int = 0) {
        val ready = _state.value as? AlbumUiState.Ready ?: return
        val owned = ready.local.filterNotNull()
        if (owned.isEmpty()) return
        val chosen = ready.local.getOrNull(fromAlbumIndex)
        player.play(owned, owned.indexOf(chosen).coerceAtLeast(0))
    }

    suspend fun image(url: String, sizePx: Int): Bitmap? = deezer.image(url, sizePx)
}
