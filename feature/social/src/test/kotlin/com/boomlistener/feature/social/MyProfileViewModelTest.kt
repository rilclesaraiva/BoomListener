// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.social

import com.boomlistener.data.releases.FollowRepository
import com.boomlistener.data.releases.FollowedArtist
import com.boomlistener.data.social.SocialAccount
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

@OptIn(ExperimentalCoroutinesApi::class)
class MyProfileViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun juntaNumerosMaisOuvidasESeguidosETocaDoPerfil() = runTest(dispatcher) {
        val social = FakeSocial().apply { account.value = SocialAccount("me", "rilcle", "Rilcle", "t", sharing = false) }
        val at = LocalDateTime.of(2026, 10, 17, 20, 0).atZone(zone).toInstant().toEpochMilli()
        val history = FakeHistory(listOf(HistoryEntry(acai.uri, acai.title, acai.artist, acai.album, at)))
        val top = listOf(SongStat(acai.uri, acai.title, acai.artist, acai.album, plays = 7, lastPlayedMs = at))
        val player = FakePlayer()
        val vm = MyProfileViewModel(
            social, history, FakeStats(top), FakeLibrary(), FakeFollows(), player,
            zone = zone, today = { LocalDate.of(2026, 10, 17) },
        )
        backgroundScope.launch(dispatcher) { vm.state.collect {} }

        val state = vm.state.value!!
        assertEquals("rilcle", state.account?.handle)
        assertEquals(1, state.stats.monthPlays)
        assertEquals(3, state.stats.monthMinutes) // Açaí tem 200 s
        assertEquals(listOf("Djavan"), state.following.map { it.name })

        vm.playTop(0)
        assertEquals(listOf(acai), player.played)

        vm.setSharing(true)
        assertEquals(true, social.account.value?.sharing)
    }
}
