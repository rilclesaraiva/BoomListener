// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.releases

import android.graphics.Bitmap
import com.boomlistener.core.model.ExternalArtist
import com.boomlistener.core.model.Release
import com.boomlistener.core.model.ReleaseType
import com.boomlistener.data.deezer.AlbumResult
import com.boomlistener.data.deezer.ArtistReleasesResult
import com.boomlistener.data.deezer.DeezerRepository
import com.boomlistener.data.deezer.ProfileResult
import com.boomlistener.data.releases.FollowRepository
import com.boomlistener.data.releases.FollowedArtist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

private fun release(id: Long, type: ReleaseType, date: String) = Release(id, "R$id", type, date, null, null)
private fun artist(id: Long, name: String) = ExternalArtist(id, name, null, 0, 0, null)

private class FakeFollows(initial: List<FollowedArtist>) : FollowRepository {
    override val followed = MutableStateFlow(initial)
    override suspend fun follow(artist: FollowedArtist) = followed.update { it + artist }
    override suspend fun unfollow(deezerId: Long) = followed.update { list -> list.filterNot { it.deezerId == deezerId } }
}

private class FakeDeezer(private val byId: Map<Long, ArtistReleasesResult>) : DeezerRepository {
    override suspend fun artistProfile(artistName: String) = ProfileResult.NotFound
    override suspend fun album(albumId: Long) = AlbumResult.Failed
    override suspend fun image(url: String, sizePx: Int): Bitmap? = null
    override suspend fun artistReleases(artistId: Long) = byId[artistId] ?: ArtistReleasesResult.Failed
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReleasesViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val today = LocalDate.of(2026, 10, 17)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val deezer = FakeDeezer(
        mapOf(
            1L to ArtistReleasesResult.Found(
                artist(1, "Chikoi The Maid"),
                listOf(release(10, ReleaseType.Single, "2026-10-10"), release(11, ReleaseType.Album, "2024-03-01")),
            ),
            2L to ArtistReleasesResult.Found(
                artist(2, "Djavan"),
                listOf(release(20, ReleaseType.EP, "2025-06-01"), release(10, ReleaseType.Single, "2026-10-10")),
            ),
        ),
    )

    @Test
    fun maisRecentesPrimeiroSemRepetirEFiltra() = runTest(dispatcher) {
        val vm = ReleasesViewModel(FakeFollows(listOf(FollowedArtist(1, "Chikoi"), FollowedArtist(2, "Djavan"))), deezer, today = { today })
        backgroundScope.launch(dispatcher) { vm.state.collect {} }

        val ready = vm.state.value as ReleasesUiState.Ready
        assertEquals(listOf(10L, 20L, 11L), ready.latest.map { it.release.id })
        // Por artista: quem lançou por último vem primeiro.
        assertEquals(listOf("Chikoi The Maid", "Djavan"), ready.byArtist.map { it.artist.name })

        vm.setFilter(ReleaseFilter.Albums)
        assertEquals(listOf(11L), (vm.state.value as ReleasesUiState.Ready).latest.map { it.release.id })
        assertEquals(listOf("Chikoi The Maid"), (vm.state.value as ReleasesUiState.Ready).byArtist.map { it.artist.name })
    }

    @Test
    fun semSeguirNinguem() = runTest(dispatcher) {
        val vm = ReleasesViewModel(FakeFollows(emptyList()), deezer, today = { today })
        backgroundScope.launch(dispatcher) { vm.state.collect {} }
        assertEquals(ReleasesUiState.NoFollows, vm.state.value)
    }

    @Test
    fun umArtistaFalhouOsOutrosAparecem() = runTest(dispatcher) {
        val vm = ReleasesViewModel(FakeFollows(listOf(FollowedArtist(1, "Chikoi"), FollowedArtist(99, "?"))), deezer, today = { today })
        backgroundScope.launch(dispatcher) { vm.state.collect {} }
        val ready = vm.state.value as ReleasesUiState.Ready
        assertEquals(1, ready.failed)
        assertEquals(2, ready.latest.size)
    }

    @Test
    fun tudoFalhou() = runTest(dispatcher) {
        val vm = ReleasesViewModel(FakeFollows(listOf(FollowedArtist(99, "?"))), deezer, today = { today })
        backgroundScope.launch(dispatcher) { vm.state.collect {} }
        assertEquals(ReleasesUiState.Failed, vm.state.value)
    }

    @Test
    fun novoNosUltimos30Dias() {
        assertTrue(isNewRelease(release(1, ReleaseType.Single, "2026-10-01"), today))
        assertTrue(isNewRelease(release(1, ReleaseType.Single, "2026-11-20"), today)) // pré-lançamento
        assertFalse(isNewRelease(release(1, ReleaseType.Single, "2026-08-01"), today))
        assertFalse(isNewRelease(Release(1, "x", ReleaseType.Album, null, null, null), today))
    }
}
