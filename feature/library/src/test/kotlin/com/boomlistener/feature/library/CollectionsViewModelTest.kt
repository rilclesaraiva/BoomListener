// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import com.boomlistener.core.model.Playlist
import com.boomlistener.core.model.Track
import com.boomlistener.data.library.PlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

internal class FakePlaylists(initial: List<Playlist> = emptyList()) : PlaylistRepository {
    val state = MutableStateFlow(initial)
    override val playlists: Flow<List<Playlist>> = state
    override suspend fun create(name: String, trackUris: List<String>): Long {
        val id = (state.value.maxOfOrNull { it.id } ?: 0) + 1
        state.value = state.value + Playlist(id, name, trackUris, 0, 0)
        return id
    }
    override suspend fun rename(id: Long, name: String) {
        state.value = state.value.map { if (it.id == id) it.copy(name = name) else it }
    }
    override suspend fun delete(id: Long) {
        state.value = state.value.filterNot { it.id == id }
    }
    override suspend fun addTracks(id: Long, uris: List<String>): Int {
        var added = 0
        state.value = state.value.map { p ->
            if (p.id != id) return@map p
            val new = uris.filterNot { it in p.trackUris }
            added = new.size
            p.copy(trackUris = p.trackUris + new)
        }
        return added
    }
    override suspend fun removeTracks(id: Long, uris: List<String>): Int {
        var removed = 0
        state.value = state.value.map { p ->
            if (p.id != id) return@map p
            val kept = p.trackUris.filterNot { it in uris }
            removed = p.trackUris.size - kept.size
            p.copy(trackUris = kept)
        }
        return removed
    }
    override suspend fun removeAt(id: Long, index: Int) {
        state.value = state.value.map { p -> if (p.id == id) p.copy(trackUris = p.trackUris.filterIndexed { i, _ -> i != index }) else p }
    }
    override suspend fun move(id: Long, from: Int, to: Int) {
        state.value = state.value.map { p ->
            if (p.id == id) p.copy(trackUris = p.trackUris.toMutableList().apply { add(to, removeAt(from)) }) else p
        }
    }
}

private fun song(id: Long, title: String, artist: String?, album: String?, albumId: Long, genre: String?) =
    Track(id, "content://$id", title, artist, album, albumId, 1000, id.toInt(), 2020, 0, genre)

internal val sampleTracks = listOf(
    song(1, "Açaí", "Djavan", "Luz", 9, "MPB"),
    song(2, "Samurai", "Djavan", "Luz", 9, "MPB"),
    song(3, "Catharsis", "Chikoi The Maid", "Insertion", 4, "Math Rock"),
)

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionsViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun separaEmGenerosArtistasAlbunsEPlaylists() = runTest(dispatcher) {
        val playlists = FakePlaylists(listOf(Playlist(1, "Viagem", listOf("content://3", "content://apagada"), 0, 0)))
        val vm = CollectionsViewModel(FakeLibraryRepository(sampleTracks), playlists)
        backgroundScope.launch(dispatcher) { vm.state.collect {} }

        val state = vm.state.value as CollectionsUiState.Ready
        assertEquals(CollectionKind.Genres, state.selected)
        assertEquals(listOf("Math Rock", "MPB"), state.genres.map { it.name })
        assertEquals(listOf("Chikoi The Maid", "Djavan"), state.artists.map { it.name })
        assertEquals(listOf("Insertion", "Luz"), state.albums.map { it.name })
        // Música que saiu do aparelho não aparece na playlist.
        assertEquals(listOf("Catharsis"), state.playlists.single().tracks.map { it.title })

        vm.select(CollectionKind.Albums)
        assertEquals(CollectionKind.Albums, (vm.state.value as CollectionsUiState.Ready).selected)

        vm.createPlaylist("Correr")
        assertEquals(2, (vm.state.value as CollectionsUiState.Ready).playlists.size)
    }

    @Test
    fun gradeOuListaLembradaPorPilula() = runTest(dispatcher) {
        val layout = com.boomlistener.data.library.MemoryCollectionsLayout()
        val vm = CollectionsViewModel(FakeLibraryRepository(sampleTracks), FakePlaylists(emptyList()), layout = layout)
        // Padrão: só os álbuns em grade.
        assertEquals(setOf("albums"), vm.gridKinds.value)
        vm.setGrid(CollectionKind.Artists, true)
        vm.setGrid(CollectionKind.Albums, false)
        assertEquals(setOf("artists"), layout.gridKinds.value)
    }
}
