// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import com.boomlistener.core.model.Playlist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionDetailViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun albumEmOrdemETocaAPartirDaEscolhida() = runTest(dispatcher) {
        val player = FakePlaybackController()
        val vm = CollectionDetailViewModel(CollectionRef.Album("9"), FakeLibraryRepository(sampleTracks), FakePlaylists(), player)
        backgroundScope.launch(dispatcher) { vm.state.collect {} }

        val state = vm.state.value as CollectionDetailUiState.Ready
        assertEquals("Luz", state.name)
        assertEquals("Djavan", state.subtitle)
        assertEquals(listOf("Açaí", "Samurai"), state.tracks.map { it.title })
        vm.play(1)
        assertEquals(1, player.lastIndex)
    }

    @Test
    fun playlistReordenaTiraERenomeia() = runTest(dispatcher) {
        val playlists = FakePlaylists(listOf(Playlist(7, "Viagem", listOf("content://1", "content://2", "content://3"), 0, 0)))
        val vm = CollectionDetailViewModel(CollectionRef.Playlist(7), FakeLibraryRepository(sampleTracks), playlists, FakePlaybackController())
        backgroundScope.launch(dispatcher) { vm.state.collect {} }

        vm.move("content://3", "content://1")
        assertEquals(listOf("content://3", "content://1", "content://2"), playlists.state.value.single().trackUris)
        val second = (vm.state.value as CollectionDetailUiState.Ready).tracks[1]
        vm.remove(second)
        assertEquals(listOf("content://3", "content://2"), playlists.state.value.single().trackUris)
        vm.rename("Estrada")
        assertEquals("Estrada", (vm.state.value as CollectionDetailUiState.Ready).name)
        vm.delete()
        assertEquals(CollectionDetailUiState.Missing, vm.state.value)
    }

    @Test
    fun generoQueNaoExisteMais() = runTest(dispatcher) {
        val vm = CollectionDetailViewModel(CollectionRef.Genre("forro"), FakeLibraryRepository(sampleTracks), FakePlaylists(), FakePlaybackController())
        backgroundScope.launch(dispatcher) { vm.state.collect {} }
        assertEquals(CollectionDetailUiState.Missing, vm.state.value)
    }
}
