// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boomlistener.core.common.ExternalLinks
import com.boomlistener.core.model.Track
import com.boomlistener.core.player.PlaybackController
import com.boomlistener.data.library.LibraryRepository
import com.boomlistener.data.library.PlaylistRepository
import com.boomlistener.data.library.groupByAlbum
import com.boomlistener.data.library.groupByGenre
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Qual coleção abrir. */
internal sealed interface CollectionRef {
    data class Genre(val key: String) : CollectionRef
    data class Album(val key: String) : CollectionRef
    data class Playlist(val id: Long) : CollectionRef
}

internal sealed interface CollectionDetailUiState {
    data object Loading : CollectionDetailUiState
    /** A coleção não existe mais (playlist apagada, álbum sem músicas). */
    data object Missing : CollectionDetailUiState
    data class Ready(
        val name: String,
        val subtitle: String?,
        val year: Int?,
        val tracks: List<Track>,
        val isPlaylist: Boolean,
    ) : CollectionDetailUiState
}

/** Um gênero, álbum ou playlist aberto: ouvir, embaralhar e (na playlist) editar. */
internal class CollectionDetailViewModel(
    private val ref: CollectionRef,
    private val library: LibraryRepository,
    private val playlists: PlaylistRepository,
    private val player: PlaybackController,
    private val songLink: suspend (title: String, artist: String?, durationMs: Long) -> String? = { _, _, _ -> null },
) : ViewModel() {

    private val tracks = MutableStateFlow<List<Track>?>(null)

    val state: StateFlow<CollectionDetailUiState> =
        combine(tracks, if (ref is CollectionRef.Playlist) playlists.playlists else flowOf(emptyList())) { all, lists ->
            when {
                all == null -> CollectionDetailUiState.Loading
                ref is CollectionRef.Playlist -> lists.firstOrNull { it.id == ref.id }
                    ?.let { summarize(it, all) }
                    ?.let { CollectionDetailUiState.Ready(it.playlist.name, null, null, it.tracks, isPlaylist = true) }
                    ?: CollectionDetailUiState.Missing
                ref is CollectionRef.Genre -> groupByGenre(all).firstOrNull { it.key == ref.key }
                    ?.let { CollectionDetailUiState.Ready(it.name, null, null, it.tracks, isPlaylist = false) }
                    ?: CollectionDetailUiState.Missing
                ref is CollectionRef.Album -> groupByAlbum(all).firstOrNull { it.key == ref.key }
                    ?.let { CollectionDetailUiState.Ready(it.name, it.subtitle, it.year, it.tracks, isPlaylist = false) }
                    ?: CollectionDetailUiState.Missing
                else -> CollectionDetailUiState.Missing
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CollectionDetailUiState.Loading)

    init {
        load()
        viewModelScope.launch { library.version.drop(1).collect { load() } }
    }

    private fun load() {
        viewModelScope.launch { tracks.value = library.getTracks() }
    }

    private fun shown(): List<Track> = (state.value as? CollectionDetailUiState.Ready)?.tracks.orEmpty()

    fun play(index: Int = 0) {
        val list = shown()
        if (list.isNotEmpty()) player.play(list, index.coerceIn(list.indices))
    }

    fun shuffle() {
        val list = shown()
        if (list.isNotEmpty()) player.playShuffled(list)
    }

    // ---- Só playlist ----

    fun remove(track: Track) {
        val id = (ref as? CollectionRef.Playlist)?.id ?: return
        viewModelScope.launch { playlists.removeTracks(id, listOf(track.uri)) }
    }

    /** Todas as músicas do aparelho (para "Adicionar músicas"). */
    val libraryTracks: StateFlow<List<Track>> = tracks
        .map { it.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** As músicas que já estão nesta playlist. */
    val playlistUris: StateFlow<Set<String>> =
        (if (ref is CollectionRef.Playlist) playlists.playlists else flowOf(emptyList()))
            .map { lists -> lists.firstOrNull { it.id == (ref as? CollectionRef.Playlist)?.id }?.trackUris.orEmpty().toSet() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** Marca/desmarca a música na playlist ("Adicionar músicas"). */
    fun toggleInPlaylist(uri: String) {
        val id = (ref as? CollectionRef.Playlist)?.id ?: return
        viewModelScope.launch {
            val inside = currentPlaylist()?.trackUris?.contains(uri) == true
            if (inside) playlists.removeTracks(id, listOf(uri)) else playlists.addTracks(id, listOf(uri))
        }
    }

    /** Arrastou [fromUri] para o lugar de [toUri]. */
    fun move(fromUri: String, toUri: String) {
        val id = (ref as? CollectionRef.Playlist)?.id ?: return
        viewModelScope.launch {
            val playlist = currentPlaylist() ?: return@launch
            val from = playlist.trackUris.indexOf(fromUri)
            val to = playlist.trackUris.indexOf(toUri)
            if (from >= 0 && to >= 0) playlists.move(id, from, to)
        }
    }

    fun rename(name: String) {
        val id = (ref as? CollectionRef.Playlist)?.id ?: return
        viewModelScope.launch { playlists.rename(id, name) }
    }

    fun delete() {
        val id = (ref as? CollectionRef.Playlist)?.id ?: return
        viewModelScope.launch { playlists.delete(id) }
    }

    private suspend fun currentPlaylist() =
        (ref as? CollectionRef.Playlist)?.let { wanted -> playlists.playlists.first().firstOrNull { it.id == wanted.id } }

    suspend fun shareUrl(track: Track): String =
        runCatching { songLink(track.title, track.artist, track.durationMs) }.getOrNull()
            ?: ExternalLinks.youtubeMusicSearch(track.title, track.artist)

    suspend fun artwork(uri: String, sizePx: Int): Bitmap? = library.loadArtwork(uri, sizePx)
}
