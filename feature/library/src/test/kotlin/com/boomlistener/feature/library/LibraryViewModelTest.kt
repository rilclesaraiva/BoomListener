// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import android.graphics.Bitmap
import com.boomlistener.core.model.Track
import com.boomlistener.core.player.PlaybackController
import com.boomlistener.core.player.PlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.boomlistener.data.library.LibraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeLibraryRepository(private val tracks: List<Track>) : LibraryRepository {
    override suspend fun getTracks(): List<Track> = tracks
    override suspend fun loadArtwork(trackUri: String, sizePx: Int): Bitmap? = null
}

internal class FakePlaybackController : PlaybackController {
    var lastQueue: List<Track> = emptyList()
    var lastIndex: Int = -1
    override val state: StateFlow<PlaybackState> = MutableStateFlow(PlaybackState())
    override fun play(tracks: List<Track>, startIndex: Int) {
        lastQueue = tracks
        lastIndex = startIndex
    }
    override fun togglePlayPause() = Unit
    override fun next() = Unit
    override fun previous() = Unit
    override fun seekTo(positionMs: Long) = Unit
    override fun toggleShuffle() = Unit
    override fun cycleRepeatMode() = Unit
    override fun stop() = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun comecaCarregando() = runTest(dispatcher) {
        val viewModel = LibraryViewModel(FakeLibraryRepository(emptyList()), FakePlaybackController())
        assertTrue(viewModel.uiState.value is LibraryUiState.Loading)
    }

    @Test
    fun carregaEFiltra() = runTest(dispatcher) {
        val player = FakePlaybackController()
        val viewModel = LibraryViewModel(
            FakeLibraryRepository(listOf(track(1, "Açaí"), track(2, "Oceano"), track(3, "Oração"))),
            player,
        )
        // Mantém o StateFlow ativo durante o teste.
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }

        viewModel.load()
        val loaded = viewModel.uiState.first { it is LibraryUiState.Success } as LibraryUiState.Success
        assertEquals(3, loaded.tracks.size)

        // Tocar sem busca: a fila é a lista inteira.
        viewModel.play(loaded.tracks[1])
        assertEquals(3, player.lastQueue.size)
        assertEquals(1, player.lastIndex)

        viewModel.onQueryChange("oce")
        val filtered = viewModel.uiState.value as LibraryUiState.Success
        assertEquals(listOf(2L), filtered.tracks.map { it.id })
        assertEquals(3, filtered.totalCount)

        // Tocar durante a busca: a fila é só o resultado da busca.
        viewModel.play(filtered.tracks[0])
        assertEquals(listOf(2L), player.lastQueue.map { it.id })
        assertEquals(0, player.lastIndex)
    }
}
