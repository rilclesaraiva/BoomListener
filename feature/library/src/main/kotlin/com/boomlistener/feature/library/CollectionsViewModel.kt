// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boomlistener.core.common.normalizedForSearch
import com.boomlistener.core.model.ExternalArtist
import com.boomlistener.core.model.Playlist
import com.boomlistener.core.model.Track
import com.boomlistener.data.library.LibraryRepository
import com.boomlistener.data.library.PlaylistRepository
import com.boomlistener.data.library.TrackGroup
import com.boomlistener.data.library.groupByAlbum
import com.boomlistener.data.library.groupByArtist
import com.boomlistener.data.library.groupByGenre
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    /** Busca de artistas no Deezer (null = falhou). */
    private val searchArtistsOnline: suspend (String) -> List<ExternalArtist>? = { emptyList() },
    private val remoteImage: suspend (String, Int) -> Bitmap? = { _, _ -> null },
) : ViewModel() {

    private val _artistSearch = MutableStateFlow(ArtistSearch())

    /** Busca da pílula Artistas: os do aparelho que batem e, do Deezer, os que você não tem. */
    val artistSearch: StateFlow<ArtistSearch> = _artistSearch.asStateFlow()
    private var searchJob: Job? = null

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

    fun onArtistQueryChange(query: String) {
        _artistSearch.value = _artistSearch.value.copy(query = query)
        searchJob?.cancel()
        if (query.isBlank()) {
            _artistSearch.value = ArtistSearch(query = query)
            return
        }
        _artistSearch.value = _artistSearch.value.copy(loading = true, failed = false)
        searchJob = viewModelScope.launch {
            // Espera parar de digitar um pouco: uma consulta por palavra, não por letra.
            delay(ARTIST_SEARCH_DELAY_MS)
            val results = searchArtistsOnline(query)
            _artistSearch.value = _artistSearch.value.copy(
                loading = false,
                failed = results == null,
                online = results.orEmpty(),
            )
        }
    }

    suspend fun artistPicture(url: String, sizePx: Int): Bitmap? = remoteImage(url, sizePx)

    fun createPlaylist(name: String) {
        viewModelScope.launch { playlistRepository.create(name) }
    }

    suspend fun artwork(uri: String, sizePx: Int): Bitmap? = library.loadArtwork(uri, sizePx)
}

/** Busca de artistas na pílula Artistas. */
internal data class ArtistSearch(
    val query: String = "",
    val loading: Boolean = false,
    val failed: Boolean = false,
    /** Tudo o que o Deezer achou (a tela tira os que você já tem). */
    val online: List<ExternalArtist> = emptyList(),
)

private const val ARTIST_SEARCH_DELAY_MS = 400L

/** Os artistas do aparelho cujo nome tem [query] (sem diferença de acento ou maiúscula). */
internal fun matchingLocalArtists(artists: List<TrackGroup>, query: String): List<TrackGroup> {
    val needle = query.normalizedForSearch().trim()
    if (needle.isEmpty()) return artists
    return artists.filter { it.name.normalizedForSearch().contains(needle) }
}

/** Do que o Deezer achou, só quem não está no aparelho (o mesmo nome, sem acento). */
internal fun onlineArtistsNotOwned(online: List<ExternalArtist>, local: List<TrackGroup>): List<ExternalArtist> {
    val owned = local.mapTo(HashSet()) { it.name.normalizedForSearch().trim() }
    return online.filter { it.name.normalizedForSearch().trim() !in owned }.distinctBy { it.id }
}

/** As músicas da playlist, na ordem dela, sem as que saíram do aparelho. */
internal fun summarize(playlist: Playlist, library: List<Track>): PlaylistSummary {
    val byUri = library.associateBy { it.uri }
    return PlaylistSummary(playlist, playlist.trackUris.mapNotNull { byUri[it] })
}
