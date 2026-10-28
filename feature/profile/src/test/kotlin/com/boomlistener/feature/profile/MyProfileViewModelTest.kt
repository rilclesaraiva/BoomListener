// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.profile

import com.boomlistener.data.releases.FollowRepository
import com.boomlistener.data.releases.FollowedArtist
import android.graphics.Bitmap
import com.boomlistener.core.model.Track
import com.boomlistener.core.player.PlaybackController
import com.boomlistener.core.player.PlaybackState
import com.boomlistener.data.library.LibraryRepository
import com.boomlistener.data.profile.MAX_BIO
import com.boomlistener.data.profile.MyProfile
import com.boomlistener.data.profile.ProfileImage
import com.boomlistener.data.profile.ProfileRepository
import com.boomlistener.data.profile.cleanProfileColor
import kotlinx.coroutines.flow.StateFlow
import com.boomlistener.data.wellbeing.HistoryEntry
import com.boomlistener.data.wellbeing.ListeningHistoryRepository
import com.boomlistener.data.wellbeing.ListeningStatsRepository
import com.boomlistener.data.wellbeing.SongStat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
import java.time.LocalDateTime
import java.time.ZoneId

private val zone = ZoneId.of("America/Belem")

private class FakeHistory(entries: List<HistoryEntry>) : ListeningHistoryRepository {
    override val history: Flow<List<HistoryEntry>> = MutableStateFlow(entries)
    override suspend fun record(entry: HistoryEntry) = Unit
    override suspend fun clear() = Unit
}

private class FakeStats(private val top: List<SongStat>) : ListeningStatsRepository {
    override fun topSongs(limit: Int): Flow<List<SongStat>> = flowOf(top)
    override suspend fun recordPlay(uri: String, title: String, artist: String?, album: String?, timestampMs: Long) = Unit
}

private class FakeFollows : FollowRepository {
    override val followed = MutableStateFlow(listOf(FollowedArtist(1, "Djavan")))
    override suspend fun follow(artist: FollowedArtist) = Unit
    override suspend fun unfollow(deezerId: Long) = Unit
}

internal val acai = Track(1, "content://1", "Açaí", "Djavan", "Luz", 9, 200_000, 1, 1982, 0)

internal class FakeLibrary : LibraryRepository {
    override suspend fun getTracks() = listOf(acai)
    override suspend fun loadArtwork(trackUri: String, sizePx: Int): Bitmap? = null
}

internal class FakePlayer : PlaybackController {
    var played: List<Track> = emptyList()
    override val state: StateFlow<PlaybackState> = MutableStateFlow(PlaybackState())
    override fun play(tracks: List<Track>, startIndex: Int) { played = tracks }
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

/** Perfil na memória; [failImages] simula uma imagem que não deu para ler. */
private class FakeProfile : ProfileRepository {
    override val profile = MutableStateFlow(MyProfile(name = "Rilcle"))
    val imageCalls = mutableListOf<Pair<ProfileImage, String?>>()
    var failImages = false

    override suspend fun save(name: String, bio: String, color: String, solidColor: Boolean): Boolean {
        val clean = cleanProfileColor(color) ?: return false
        profile.value = profile.value.copy(name = name, bio = bio.take(MAX_BIO), color = clean, solidColor = solidColor)
        return true
    }

    override suspend fun setImage(kind: ProfileImage, uri: String?): Boolean {
        imageCalls += kind to uri
        if (failImages) return false
        val version = if (uri == null) 0L else 99L
        profile.value = if (kind == ProfileImage.Banner) profile.value.copy(bannerVersion = version) else profile.value.copy(avatarVersion = version)
        return true
    }

    override suspend fun image(kind: ProfileImage): ByteArray? = null
}

@OptIn(ExperimentalCoroutinesApi::class)
class MyProfileViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun juntaNumerosMaisOuvidasESeguidosETocaDoPerfil() = runTest(dispatcher) {
        val at = LocalDateTime.of(2026, 10, 17, 20, 0).atZone(zone).toInstant().toEpochMilli()
        val history = FakeHistory(listOf(HistoryEntry(acai.uri, acai.title, acai.artist, acai.album, at)))
        val top = listOf(SongStat(acai.uri, acai.title, acai.artist, acai.album, plays = 7, lastPlayedMs = at))
        val player = FakePlayer()
        val vm = MyProfileViewModel(
            FakeProfile(), history, FakeStats(top), FakeLibrary(), FakeFollows(), player,
            zone = zone, today = { LocalDate.of(2026, 10, 17) },
        )
        backgroundScope.launch(dispatcher) { vm.state.collect {} }

        val state = vm.state.value!!
        assertEquals("Rilcle", state.profile.name)
        assertEquals(1, state.stats.monthPlays)
        assertEquals(3, state.stats.monthMinutes) // Açaí tem 200 s
        assertEquals(listOf("Djavan"), state.following.map { it.name })

        vm.playTop(0)
        assertEquals(listOf(acai), player.played)
    }

    @Test
    fun fotoDeArtistaNaoSeguidoVemDoDeezerPeloNome() = runTest(dispatcher) {
        val byName = mutableListOf<String>()
        val byUrl = mutableListOf<String>()
        val vm = MyProfileViewModel(
            FakeProfile(), FakeHistory(emptyList()), FakeStats(emptyList()), FakeLibrary(), FakeFollows(), FakePlayer(),
            remoteImage = { url, _ -> byUrl += url; null },
            artistPhoto = { name, _ -> byName += name; null },
            zone = zone, today = { LocalDate.of(2026, 10, 17) },
        )
        backgroundScope.launch(dispatcher) { vm.state.collect {} }

        // Não seguido: procura pelo nome.
        vm.artistImage("Sh0tty", 120)
        assertEquals(listOf("Sh0tty"), byName)
        assertTrue(byUrl.isEmpty())

        // Seguido: vai direto pelo id, sem procurar.
        vm.artistImage("Djavan", 120)
        assertEquals(listOf("Sh0tty"), byName)
        assertEquals(1, byUrl.size)
    }

    @Test
    fun nadaMudaAteSalvar() = runTest(dispatcher) {
        val profile = FakeProfile()
        val vm = MyProfileViewModel(
            profile, FakeHistory(emptyList()), FakeStats(emptyList()), FakeLibrary(), FakeFollows(), FakePlayer(),
            zone = zone, today = { LocalDate.of(2026, 10, 17) },
        )
        backgroundScope.launch(dispatcher) { vm.state.collect {} }

        // Só "Salvar" grava: descrição (cortada nos 160), cor, capa nova e foto tirada.
        var done = false
        vm.saveProfile(
            name = "Rilcle",
            bio = "x".repeat(200),
            banner = ImageChange.Set("content://media/picker/2"),
            avatar = ImageChange.Remove,
            color = "#FF5500",
            onDone = { done = true },
        )
        assertTrue(done)
        assertEquals("#FF5500", profile.profile.value.color)
        assertEquals(MAX_BIO, profile.profile.value.bio.length)
        assertEquals(99L, profile.profile.value.bannerVersion)
        assertEquals(listOf(ProfileImage.Banner to "content://media/picker/2", ProfileImage.Avatar to null), profile.imageCalls)
        assertFalse(vm.saving.value)

        // Sem trocar imagem: nada de mexer nelas.
        profile.imageCalls.clear()
        vm.saveProfile("Rilcle", "outra", ImageChange.Keep, ImageChange.Keep, onDone = {})
        assertTrue(profile.imageCalls.isEmpty())

        // Imagem que não deu para ler: avisa e não fecha.
        profile.failImages = true
        var closed = false
        vm.saveProfile("Rilcle", "outra", ImageChange.Keep, ImageChange.Set("content://x"), onDone = { closed = true })
        assertTrue(vm.saveFailed.value)
        assertFalse(closed)
    }

    @Test
    fun artistaPrincipalEFotoDoDeezer() {
        assertEquals("Djavan", artistOf("Djavan feat. Caetano Veloso"))
        assertEquals("Chikoi The Maid", artistOf("Chikoi The Maid ft. Alguém"))
        assertEquals("Featherstone", artistOf("Featherstone"))
        assertEquals("https://api.deezer.com/artist/27/image?size=medium", deezerArtistImageUrl(27))
    }
}
