// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.artist

import android.graphics.Bitmap
import com.boomlistener.core.model.ArtistProfile
import com.boomlistener.core.model.ExternalArtist
import com.boomlistener.core.model.PopularTrack
import com.boomlistener.core.model.Release
import com.boomlistener.core.model.ReleaseType
import com.boomlistener.core.model.Track
import com.boomlistener.core.player.PlaybackController
import com.boomlistener.core.player.PlaybackState
import com.boomlistener.core.model.AlbumDetail
import com.boomlistener.data.deezer.AlbumResult
import com.boomlistener.data.deezer.DeezerRepository
import com.boomlistener.data.deezer.ProfileResult
import com.boomlistener.data.lastfm.ArtistBio
import com.boomlistener.data.lastfm.BioResult
import com.boomlistener.data.lastfm.LastfmRepository
import com.boomlistener.data.library.LibraryRepository
import com.boomlistener.data.releases.FollowRepository
import com.boomlistener.data.releases.FollowedArtist
import com.boomlistener.data.releases.ReleaseAlertsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

private fun track(id: Long, title: String, artist: String?, album: String?) = Track(
    id, "content://$id", title, artist, album, 0, 200_000, null, null, 0,
)

private fun release(id: Long, title: String, type: ReleaseType = ReleaseType.Album) =
    Release(id, title, type, "2020-01-01", null, null)

class ArtistLogicTest {
    private val library = listOf(
        track(1, "Açaí", "Djavan", "Luz"),
        track(2, "Sina", "Djavan feat. Caetano Veloso", "Luz (Remastered)"),
        track(3, "Oceano", "DJAVAN", "Djavan"),
        track(4, "Garota de Ipanema", "João Gilberto", "Getz/Gilberto"),
    )

    @Test
    fun musicasDoArtistaIncluemParticipacaoEMaiusculas() {
        assertEquals(listOf(1L, 2L, 3L), tracksOfArtist(library, "Djavan").map { it.id })
    }

    @Test
    fun albunsContadosSemRepetirEdicoes() {
        assertEquals(2, localAlbumCount(tracksOfArtist(library, "Djavan")))
    }

    @Test
    fun discografiaMarcaOQueVoceTem() {
        val releases = listOf(
            release(10, "Luz (Deluxe Edition)"),
            release(11, "Djavan"),
            release(12, "Coisa de Acender"),
            release(13, "Oceano - Single", ReleaseType.Single),
        )
        val owned = ownedReleaseIds(releases, tracksOfArtist(library, "Djavan"))
        assertEquals(setOf(10L, 11L, 13L), owned)
    }

    @Test
    fun popularesQueVoceTem() {
        val top = listOf(
            PopularTrack(20, "Açaí", null, null, 0, null),
            PopularTrack(21, "Flor de Lis", null, null, 0, null),
        )
        assertEquals(setOf(20L), ownedPopularIds(top, tracksOfArtist(library, "Djavan")))
    }
}

private class FakeDeezer(
    private val result: ProfileResult,
    private val albumResult: AlbumResult = AlbumResult.Failed,
) : DeezerRepository {
    override suspend fun artistProfile(artistName: String) = result
    override suspend fun album(albumId: Long) = albumResult
    override suspend fun image(url: String, sizePx: Int): Bitmap? = null
}

private class FakeLibrary(private val tracks: List<Track>) : LibraryRepository {
    override suspend fun getTracks() = tracks
    override suspend fun loadArtwork(trackUri: String, sizePx: Int): Bitmap? = null
}

private class FakeFollows : FollowRepository {
    override val followed = MutableStateFlow<List<FollowedArtist>>(emptyList())
    override suspend fun follow(artist: FollowedArtist) = followed.update { it + artist }
    override suspend fun unfollow(deezerId: Long) = followed.update { list -> list.filterNot { it.deezerId == deezerId } }
}

private class FakeAlerts : ReleaseAlertsRepository {
    override val enabled = MutableStateFlow(true)
    override fun setEnabled(enabled: Boolean) { this.enabled.value = enabled }
    override fun known(): Map<Long, Set<Long>> = emptyMap()
    override fun saveKnown(known: Map<Long, Set<Long>>) = Unit
    override var askedPermission: Boolean = false
    override val muted = MutableStateFlow<Set<Long>>(emptySet())
    override fun setMuted(artistId: Long, muted: Boolean) {
        this.muted.value = if (muted) this.muted.value + artistId else this.muted.value - artistId
    }
}

private class FakeLastfm(private val result: BioResult = BioResult.NotFound) : LastfmRepository {
    var askedFor: String? = null
    override suspend fun artistBio(artistName: String, language: String): BioResult {
        askedFor = artistName
        return result
    }
}

private class FakePlayer : PlaybackController {
    var shuffled: List<Track> = emptyList()
    var played: Pair<List<Track>, Int>? = null
    override val state: StateFlow<PlaybackState> = MutableStateFlow(PlaybackState())
    override fun play(tracks: List<Track>, startIndex: Int) { played = tracks to startIndex }
    override fun playShuffled(tracks: List<Track>) { shuffled = tracks }
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
class ArtistViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val profile = ArtistProfile(
        artist = ExternalArtist(27, "Djavan", null, 1000, 30, null),
        topTracks = emptyList(),
        releases = listOf(release(10, "Luz")),
        related = emptyList(),
    )

    @Test
    fun seguirETocarAleatorio() = runTest(dispatcher) {
        val follows = FakeFollows()
        val player = FakePlayer()
        val bio = ArtistBio("Cantor e compositor.", null, listOf("mpb"), 1000, null)
        val lastfm = FakeLastfm(BioResult.Found(bio))
        val viewModel = ArtistViewModel(
            "Djavan",
            FakeLibrary(listOf(track(1, "Açaí", "Djavan", "Luz"), track(2, "Outra", "Outro", "X"))),
            FakeDeezer(ProfileResult.Found(profile)),
            follows,
            player,
            lastfm,
            "pt",
        )
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }

        assertEquals(listOf(1L), viewModel.uiState.value.localTracks.map { it.id })
        assertEquals(setOf(10L), viewModel.uiState.value.ownedReleases)
        assertFalse(viewModel.uiState.value.following)
        assertEquals(BioState.Ready(bio), viewModel.uiState.value.bio)
        assertEquals("Djavan", lastfm.askedFor)

        viewModel.toggleFollow()
        assertTrue(viewModel.uiState.value.following)
        assertEquals(listOf(FollowedArtist(27, "Djavan")), follows.followed.value)

        viewModel.shuffle()
        assertEquals(listOf(1L), player.shuffled.map { it.id })
    }

    @Test
    fun sinoDoArtistaDesligaSemDeixarDeSeguir() = runTest(dispatcher) {
        val follows = FakeFollows()
        val alerts = FakeAlerts()
        val viewModel = ArtistViewModel(
            "Djavan",
            FakeLibrary(listOf(track(1, "Açaí", "Djavan", "Luz"))),
            FakeDeezer(ProfileResult.Found(profile)),
            follows,
            FakePlayer(),
            FakeLastfm(),
            "pt",
            alerts = alerts,
        )
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }

        // Seguir já liga o sino.
        viewModel.toggleFollow()
        assertTrue(viewModel.uiState.value.alertsOn)

        // Desligar o sino: continua seguindo, sem avisos dele.
        viewModel.toggleAlerts()
        assertTrue(viewModel.uiState.value.following)
        assertFalse(viewModel.uiState.value.alertsOn)
        assertEquals(setOf(27L), alerts.muted.value)

        // Deixar de seguir e seguir de novo: o sino volta ligado.
        viewModel.toggleFollow()
        viewModel.toggleFollow()
        assertTrue(viewModel.uiState.value.alertsOn)
    }
}

class AlbumLogicTest {
    @Test
    fun casaMusicasDoAlbumComAsSuas() {
        val mine = listOf(track(1, "Açaí", "Djavan", "Luz"), track(2, "Sina (Ao Vivo)", "Djavan", "Luz"))
        val album = listOf(
            PopularTrack(10, "Açaí", "Luz", null, 0, null),
            PopularTrack(11, "Pétala", "Luz", null, 0, null),
        )
        assertEquals(listOf(1L, null), matchAlbumTracks(album, mine).map { it?.id })
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AlbumViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun tocaSoOQueVoceTemComecandoPelaEscolhida() {
        val album = AlbumDetail(
            release = release(10, "Luz"),
            artistName = "Djavan",
            tracks = listOf(
                PopularTrack(1, "Samurai", "Luz", null, 0, null),
                PopularTrack(2, "Pétala", "Luz", null, 0, null),
                PopularTrack(3, "Açaí", "Luz", null, 0, null),
            ),
        )
        val mine = listOf(track(7, "Samurai", "Djavan", "Luz"), track(8, "Açaí", "Djavan", "Luz"))
        val player = FakePlayer()
        val viewModel = AlbumViewModel(
            albumId = 10,
            artistName = "Djavan",
            deezer = FakeDeezer(ProfileResult.NotFound, AlbumResult.Found(album)),
            library = FakeLibrary(mine),
            player = player,
        )
        val ready = viewModel.state.value as AlbumUiState.Ready
        assertEquals(2, ready.ownedCount)

        viewModel.playOwned(fromAlbumIndex = 2)
        assertEquals(listOf(7L, 8L), player.played?.first?.map { it.id })
        assertEquals(1, player.played?.second)
    }
}
