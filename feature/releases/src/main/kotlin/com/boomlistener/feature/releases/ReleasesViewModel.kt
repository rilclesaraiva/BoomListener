// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.releases

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boomlistener.data.deezer.ArtistReleasesResult
import com.boomlistener.data.deezer.DeezerRepository
import com.boomlistener.data.releases.FollowRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.time.LocalDate

/** Resultado da busca das discografias. */
private sealed interface Loaded {
    data object Loading : Loaded
    data object NoFollows : Loaded
    data class Done(val artists: List<ArtistReleases>, val failed: Int) : Loaded
}

internal sealed interface ReleasesUiState {
    data object Loading : ReleasesUiState
    /** Ainda não segue ninguém. */
    data object NoFollows : ReleasesUiState
    /** Segue artistas, mas nenhuma busca deu certo (sem internet). */
    data object Failed : ReleasesUiState
    data class Ready(
        val view: ReleasesView,
        val filter: ReleaseFilter,
        val latest: List<ReleaseItem>,
        val byArtist: List<ArtistReleases>,
        /** Quantos artistas não carregaram (o resto aparece normalmente). */
        val failed: Int,
        val today: LocalDate,
    ) : ReleasesUiState
}

/**
 * Aba Lançamentos: álbuns, EPs e singles dos artistas que você segue, do
 * mais novo para o mais antigo ou separados por artista.
 */
internal class ReleasesViewModel(
    private val follows: FollowRepository,
    private val deezer: DeezerRepository,
    private val today: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    private val loaded = MutableStateFlow<Loaded>(Loaded.Loading)
    private val view = MutableStateFlow(ReleasesView.Latest)
    private val filter = MutableStateFlow(ReleaseFilter.All)
    private val refreshes = MutableStateFlow(0)

    val state: StateFlow<ReleasesUiState> = combine(loaded, view, filter) { result, currentView, currentFilter ->
        when (result) {
            Loaded.Loading -> ReleasesUiState.Loading
            Loaded.NoFollows -> ReleasesUiState.NoFollows
            is Loaded.Done -> if (result.artists.isEmpty() && result.failed > 0) {
                ReleasesUiState.Failed
            } else {
                ReleasesUiState.Ready(
                    view = currentView,
                    filter = currentFilter,
                    latest = latestReleases(result.artists, currentFilter),
                    byArtist = releasesByArtist(result.artists, currentFilter),
                    failed = result.failed,
                    today = today(),
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReleasesUiState.Loading)

    init {
        viewModelScope.launch {
            combine(follows.followed, refreshes) { list, _ -> list }.collectLatest { followed ->
                if (followed.isEmpty()) {
                    loaded.value = Loaded.NoFollows
                    return@collectLatest
                }
                if (loaded.value !is Loaded.Done) loaded.value = Loaded.Loading
                // Poucas consultas de cada vez, para não estourar o limite do Deezer.
                val gate = Semaphore(4)
                val results = coroutineScope {
                    followed.map { artist ->
                        async { gate.withPermit { deezer.artistReleases(artist.deezerId) } }
                    }.awaitAll()
                }
                loaded.value = Loaded.Done(
                    artists = results.filterIsInstance<ArtistReleasesResult.Found>()
                        .map { ArtistReleases(it.artist, it.releases) },
                    failed = results.count { it is ArtistReleasesResult.Failed },
                )
            }
        }
    }

    fun setView(newView: ReleasesView) {
        view.value = newView
    }

    fun setFilter(newFilter: ReleaseFilter) {
        filter.value = newFilter
    }

    fun refresh() {
        refreshes.value += 1
    }

    suspend fun image(url: String, sizePx: Int): Bitmap? = deezer.image(url, sizePx)
}
