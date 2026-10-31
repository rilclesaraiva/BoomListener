// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.history

import android.graphics.Bitmap
import com.boomlistener.core.model.Track
import com.boomlistener.core.player.PlaybackController
import com.boomlistener.core.player.PlaybackState
import com.boomlistener.data.library.LibraryRepository
import com.boomlistener.data.wellbeing.HistoryEntry
import com.boomlistener.data.wellbeing.HistoryPeriod
import com.boomlistener.data.wellbeing.ListeningHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

private val zone = ZoneId.of("America/Belem")
private val today = LocalDate.of(2026, 10, 17)

private fun at(uri: String, day: Int, hour: Int) =
    HistoryEntry(uri, uri, null, null, LocalDateTime.of(2026, 10, day, hour, 0).atZone(zone).toInstant().toEpochMilli())

private fun track(uri: String) = Track(uri.hashCode().toLong(), uri, uri, null, null, 0, 1000, null, null, 0)

private class FakeHistory(entries: List<HistoryEntry>) : ListeningHistoryRepository {
    val flow = MutableStateFlow(entries)
    override val history: Flow<List<HistoryEntry>> = flow
    override suspend fun record(entry: HistoryEntry) = Unit
    override suspend fun clear() {
        flow.value = emptyList()
    }
}

private class FakeLibrary(private val tracks: List<Track>) : LibraryRepository {
    override suspend fun getTracks() = tracks
    override suspend fun loadArtwork(trackUri: String, sizePx: Int): Bitmap? = null
}

private class FakePlayer : PlaybackController {
    var played: Pair<List<String>, Int>? = null
    override val state: StateFlow<PlaybackState> = MutableStateFlow(PlaybackState())
    override fun play(tracks: List<Track>, startIndex: Int) {
        played = tracks.map { it.uri } to startIndex
    }
    override fun playShuffled(tracks: List<Track>) = Unit
    override fun togglePlayPause() = Unit
    override fun playQueueItem(index: Int) = Unit
    override fun moveQueueItem(from: Int, to: Int) = Unit
    override fun removeQueueItem(index: Int) = Unit
    override fun next() = Unit
    override fun previous() = Unit
    override fun seekTo(positionMs: Long) = Unit
    override fun toggleShuffle() = Unit
    override fun cycleRepeatMode() = Unit
    override fun stop() = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(history: FakeHistory, library: List<Track>, player: FakePlayer = FakePlayer()) =
        HistoryViewModel(history, FakeLibrary(library), player, zone = zone, firstDayOfWeek = DayOfWeek.SUNDAY, today = { today })

    @Test
    fun agrupaETocaOGrupoSemRepetirESemAsApagadas() = runTest(dispatcher) {
        val history = FakeHistory(listOf(at("b", 17, 20), at("a", 17, 18), at("b", 17, 10), at("apagada", 17, 9), at("c", 16, 9)))
        val player = FakePlayer()
        val vm = viewModel(history, listOf(track("a"), track("b"), track("c")), player)
        backgroundScope.launch(dispatcher) { vm.state.collect {} }

        val sections = (vm.state.value as HistoryUiState.Ready).sections
        assertEquals(listOf(HistoryPeriod.Today, HistoryPeriod.Yesterday), sections.map { it.period })

        vm.play(sections[0].entries[1], sections[0]) // tocou "a"
        assertEquals(listOf("b", "a") to 1, player.played)
    }

    @Test
    fun limpar() = runTest(dispatcher) {
        val history = FakeHistory(listOf(at("a", 17, 9)))
        val vm = viewModel(history, emptyList())
        backgroundScope.launch(dispatcher) { vm.state.collect {} }
        vm.clear()
        assertEquals(HistoryUiState.Ready(emptyList()), vm.state.value)
    }
}
