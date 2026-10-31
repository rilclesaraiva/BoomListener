// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.history

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boomlistener.core.player.PlaybackController
import com.boomlistener.data.library.LibraryRepository
import com.boomlistener.data.wellbeing.HistoryEntry
import com.boomlistener.data.wellbeing.HistorySection
import com.boomlistener.data.wellbeing.ListeningHistoryRepository
import com.boomlistener.data.wellbeing.groupHistory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

internal sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    data class Ready(val sections: List<HistorySection>) : HistoryUiState
}

/**
 * Histórico de escuta agrupado (Hoje, Ontem, Esta semana, Semana passada,
 * meses anteriores). Tocar numa música toca aquele grupo a partir dela.
 */
internal class HistoryViewModel(
    private val history: ListeningHistoryRepository,
    private val library: LibraryRepository,
    private val player: PlaybackController,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val firstDayOfWeek: DayOfWeek,
    private val today: () -> LocalDate = { LocalDate.now(zone) },
) : ViewModel() {

    val state: StateFlow<HistoryUiState> = history.history
        .map { entries -> HistoryUiState.Ready(groupHistory(entries, today(), zone, firstDayOfWeek)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState.Loading)

    /**
     * Toca [entry] com o resto do grupo como fila. Músicas que não estão mais
     * no aparelho ficam de fora; repetidas no grupo entram uma vez só.
     */
    fun play(entry: HistoryEntry, section: HistorySection) {
        viewModelScope.launch {
            val byUri = library.getTracks().associateBy { it.uri }
            val queue = section.entries.map { it.uri }.distinct().mapNotNull { byUri[it] }
            val start = queue.indexOfFirst { it.uri == entry.uri }
            if (start >= 0) player.play(queue, start)
        }
    }

    fun clear() {
        viewModelScope.launch { history.clear() }
    }

    suspend fun artwork(uri: String, sizePx: Int): Bitmap? = library.loadArtwork(uri, sizePx)
}
