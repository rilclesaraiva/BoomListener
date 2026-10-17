// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import android.graphics.Bitmap
import com.boomlistener.core.model.Track
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

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun comecaCarregando() = runTest(dispatcher) {
        val viewModel = LibraryViewModel(FakeLibraryRepository(emptyList()))
        assertTrue(viewModel.uiState.value is LibraryUiState.Loading)
    }

    @Test
    fun carregaEFiltra() = runTest(dispatcher) {
        val viewModel = LibraryViewModel(
            FakeLibraryRepository(listOf(track(1, "Açaí"), track(2, "Oceano"))),
        )
        // Mantém o StateFlow ativo durante o teste.
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }

        viewModel.load()
        val loaded = viewModel.uiState.first { it is LibraryUiState.Success } as LibraryUiState.Success
        assertEquals(2, loaded.tracks.size)

        viewModel.onQueryChange("oce")
        val filtered = viewModel.uiState.value as LibraryUiState.Success
        assertEquals(listOf(2L), filtered.tracks.map { it.id })
        assertEquals(2, filtered.totalCount)
    }
}
