// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.releases

import com.boomlistener.data.releases.MemoryReleasesChoices
import com.boomlistener.data.releases.ReleasesChoices
import com.boomlistener.data.releases.ReleasesChoicesRepository
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
        val period: ReleasePeriod,
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
    /** Filtro, modo e janela escolhidos da última vez (a tela abre como ficou). */
    private val choices: ReleasesChoicesRepository = MemoryReleasesChoices(),
    private val today: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    private val saved = choices.load()

    private val loaded = MutableStateFlow<Loaded>(Loaded.Loading)
    private val view = MutableStateFlow(enumOrDefault(saved.view, ReleasesView.Latest))
    private val filter = MutableStateFlow(enumOrDefault(saved.filter, ReleaseFilter.All))
    private val period = MutableStateFlow(enumOrDefault(saved.period, ReleasePeriod.HalfYear))
    private val refreshes = MutableStateFlow(0)

    val state: StateFlow<ReleasesUiState> = combine(loaded, view, filter, period) { result, currentView, currentFilter, currentPeriod ->
        val now = today()
        when (result) {
            Loaded.Loading -> ReleasesUiState.Loading
            Loaded.NoFollows -> ReleasesUiState.NoFollows
            is Loaded.Done -> if (result.artists.isEmpty() && result.failed > 0) {
                ReleasesUiState.Failed
            } else {
                ReleasesUiState.Ready(
                    view = currentView,
                    filter = currentFilter,
                    period = currentPeriod,
                    latest = latestReleases(result.artists, currentFilter, currentPeriod, now),
                    byArtist = releasesByArtist(result.artists, currentFilter, currentPeriod, now),
                    failed = result.failed,
                    today = now,
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
        remember()
    }

    fun setFilter(newFilter: ReleaseFilter) {
        filter.value = newFilter
        remember()
    }

    fun setPeriod(newPeriod: ReleasePeriod) {
        period.value = newPeriod
        remember()
    }

    private fun remember() {
        choices.save(ReleasesChoices(view.value.name, filter.value.name, period.value.name))
    }

    fun refresh() {
        refreshes.value += 1
    }

    suspend fun image(url: String, sizePx: Int): Bitmap? = deezer.image(url, sizePx)
}

/** A opção guardada pelo nome, ou [default] (nunca escolheu, ou a opção deixou de existir). */
internal inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: default
