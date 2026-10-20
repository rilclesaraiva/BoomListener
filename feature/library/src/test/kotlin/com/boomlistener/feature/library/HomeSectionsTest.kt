// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import com.boomlistener.core.model.Track
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

private fun song(id: Long, title: String, artist: String, album: String, albumId: Long, added: Long) = Track(
    id = id,
    uri = "content://media/external/audio/media/$id",
    title = title,
    artist = artist,
    album = album,
    albumId = albumId,
    durationMs = 200_000,
    trackNumber = null,
    year = null,
    dateAddedSeconds = added,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeSectionsTest {
    private val dispatcher = UnconfinedTestDispatcher()

    private val library = listOf(
        song(1, "Rush", "Seatbelts", "Cowboy Bebop", 10, added = 100),
        song(2, "Airborn", "Chikoi The Maid", "I have never seen clouds", 20, added = 300),
        song(3, "Bus", "Chikoi The Maid", "From The Past to The Future", 30, added = 200),
        song(4, "1406", "Mamonas Assassinas", "Mamonas Assassinas", 40, added = 400),
        song(5, "Vendor", "Chikoi The Maid feat. Alguém", "From The Past to The Future", 30, added = 250),
    )

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun recentesDaMaisNovaParaAMaisAntiga() {
        val home = buildHomeSections(library, recentCount = 3)
        assertEquals(listOf(4L, 2L, 5L), home.recent.map { it.id })
    }

    @Test
    fun albunsPeloQueChegouPorUltimo() {
        val home = buildHomeSections(library)
        assertEquals(
            listOf("Mamonas Assassinas", "I have never seen clouds", "From The Past to The Future", "Cowboy Bebop"),
            home.albums.map { it.name },
        )
    }

    @Test
    fun artistasComMaisMusicasPrimeiroSemFeat() {
        val home = buildHomeSections(library)
        assertEquals("Chikoi The Maid", home.artists.first().name)
        assertEquals(3, home.artists.first().tracks.size)
    }

    @Test
    fun bibliotecaVaziaNaoTemTopo() {
        assertTrue(buildHomeSections(emptyList()).isEmpty)
    }

    @Test
    fun topoFicaNaBuscaETocarRecentesUsaAOrdemDeChegada() = runTest(dispatcher) {
        val player = FakePlaybackController()
        val viewModel = LibraryViewModel(FakeLibraryRepository(library), player)
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        viewModel.load()
        val loaded = viewModel.uiState.first { it is LibraryUiState.Success } as LibraryUiState.Success
        assertEquals(5, loaded.home?.recent?.size)

        viewModel.playRecent(1)
        assertEquals(listOf(4L, 2L, 5L, 3L, 1L), player.lastQueue.map { it.id })
        assertEquals(1, player.lastIndex)

        viewModel.playAll()
        assertEquals(0, player.lastIndex)
        assertEquals(5, player.lastQueue.size)

        // Buscando, o topo continua (a barra de busca não muda de lugar).
        viewModel.onQueryChange("bus")
        val searching = viewModel.uiState.value as LibraryUiState.Success
        assertEquals(5, searching.home?.recent?.size)
        assertEquals(listOf(3L), searching.tracks.map { it.id })
    }
}
