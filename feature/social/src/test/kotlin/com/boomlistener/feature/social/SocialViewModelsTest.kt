// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.social

import android.graphics.Bitmap
import com.boomlistener.core.model.Track
import com.boomlistener.core.player.PlaybackController
import com.boomlistener.core.player.PlaybackState
import com.boomlistener.data.deezer.AlbumResult
import com.boomlistener.data.deezer.DeezerRepository
import com.boomlistener.data.deezer.ProfileResult
import com.boomlistener.data.library.LibraryRepository
import com.boomlistener.data.social.FriendProfile
import com.boomlistener.data.social.FriendsState
import com.boomlistener.data.social.MusicCard
import com.boomlistener.data.social.Recommendation
import com.boomlistener.data.social.Relation
import com.boomlistener.data.social.HandleCheck
import com.boomlistener.data.social.LinkStatus
import com.boomlistener.data.social.SocialLive
import com.boomlistener.data.social.MusicKind
import com.boomlistener.data.social.ProfileImage
import com.boomlistener.data.social.SocialAccount
import com.boomlistener.data.social.SocialRepository
import com.boomlistener.data.social.SocialResult
import com.boomlistener.data.social.SocialUser
import com.boomlistener.data.social.TrackInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private val ana = SocialUser("a1", "ana", "Ana")

internal class FakeSocial : SocialRepository {
    override val account = MutableStateFlow<SocialAccount?>(null)
    var blocked = false
    var relation = Relation.None
    val reacted = mutableListOf<Pair<String, String>>()
    override suspend fun checkHandle(handle: String) = HandleCheck.Available
    override suspend fun register(name: String, handle: String): SocialResult<SocialAccount> {
        val created = SocialAccount("me", handle, name, "t", sharing = false)
        account.value = created
        return SocialResult.Ok(created)
    }
    override suspend fun updateProfile(name: String?, sharing: Boolean?): SocialResult<SocialAccount> {
        val updated = account.value!!.copy(sharing = sharing ?: account.value!!.sharing)
        account.value = updated
        return SocialResult.Ok(updated)
    }
    var online = true
    val imageCalls = mutableListOf<Pair<ProfileImage, String?>>()
    override suspend fun updateBio(bio: String): SocialResult<SocialAccount> {
        if (!online) return SocialResult.Offline
        val updated = account.value!!.copy(bio = bio)
        account.value = updated
        return SocialResult.Ok(updated)
    }
    override suspend fun updateColor(color: String): SocialResult<SocialAccount> {
        if (!online) return SocialResult.Offline
        val updated = account.value!!.copy(color = color)
        account.value = updated
        return SocialResult.Ok(updated)
    }
    override suspend fun setProfileImage(kind: ProfileImage, uri: String?): SocialResult<SocialAccount> {
        imageCalls += kind to uri
        if (!online) return SocialResult.Offline
        val version = if (uri == null) 0L else 99L
        val updated = when (kind) {
            ProfileImage.Avatar -> account.value!!.copy(avatarVersion = version)
            ProfileImage.Banner -> account.value!!.copy(bannerVersion = version)
        }
        account.value = updated
        return SocialResult.Ok(updated)
    }
    override suspend fun deleteAccount() = SocialResult.Ok(Unit)
    override suspend fun findByHandle(handle: String): SocialResult<FriendProfile> = SocialResult.NotFound
    override suspend fun profile(userId: String): SocialResult<FriendProfile> =
        SocialResult.Ok(
            FriendProfile(
                ana, sharing = true, now = TrackInfo("Oceano", "Djavan", null), updatedMs = 1, recent = emptyList(),
                blockedByMe = blocked, relation = relation,
            ),
        )
    var fetchedPeople: List<FriendProfile> = emptyList()
    var syncs = 0
    override val live = MutableStateFlow(SocialLive())
    override val friends = MutableStateFlow(FriendsState())
    override val recommendations = MutableStateFlow<List<Recommendation>>(emptyList())
    override suspend fun refreshFriends(): SocialResult<FriendsState> {
        syncs++
        friends.value = friends.value.copy(friends = fetchedPeople)
        return SocialResult.Ok(friends.value)
    }
    override suspend fun addFriend(userId: String): SocialResult<Relation> {
        relation = if (relation == Relation.Received) Relation.Friend else Relation.Sent
        return SocialResult.Ok(relation)
    }
    override suspend fun removeFriend(userId: String): SocialResult<Relation> {
        relation = Relation.None
        return SocialResult.Ok(relation)
    }
    override suspend fun react(toUserId: String, emoji: String, title: String?): Boolean {
        reacted += toUserId to emoji
        return live.value.status == LinkStatus.Online
    }
    override suspend fun activity(): SocialResult<List<FriendProfile>> = SocialResult.Ok(fetchedPeople)
    override suspend fun block(userId: String): SocialResult<Unit> { blocked = true; return SocialResult.Ok(Unit) }
    override suspend fun unblock(userId: String): SocialResult<Unit> { blocked = false; return SocialResult.Ok(Unit) }
    override suspend fun publishNowPlaying(track: TrackInfo?) = Unit
    override suspend fun publishListened(track: TrackInfo) = Unit
}

internal val acai = Track(1, "content://1", "Açaí", "Djavan", "Luz", 9, 200_000, 1, 1982, 0)

internal class FakeLibrary : LibraryRepository {
    override suspend fun getTracks() = listOf(acai)
    override suspend fun loadArtwork(trackUri: String, sizePx: Int): Bitmap? = null
}

private object NoDeezer : DeezerRepository {
    override suspend fun artistProfile(artistName: String) = ProfileResult.NotFound
    override suspend fun album(albumId: Long) = AlbumResult.Failed
    override suspend fun image(url: String, sizePx: Int): Bitmap? = null
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

@OptIn(ExperimentalCoroutinesApi::class)
class SocialViewModelsTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun cadastroLigaCompartilhamentoSoSeEscolhido() = runTest(dispatcher) {
        val social = FakeSocial()
        val vm = SocialHomeViewModel(social)
        vm.register("Rilcle", "rilcle", share = false)
        assertEquals(false, social.account.value?.sharing)
        vm.register("Rilcle", "rilcle", share = true)
        assertEquals(true, social.account.value?.sharing)
    }

    @Test
    fun recomendacaoTocaSeVoceTiver() = runTest(dispatcher) {
        val social = FakeSocial()
        val player = FakePlayer()
        val vm = SocialHomeViewModel(social, library = { listOf(acai) }, player = player)
        assertTrue(vm.play(MusicCard(MusicKind.Track, "ACAI", "djavan", null, null, null)))
        assertEquals(listOf(acai), player.played)
        assertNull(vm.localMatch(MusicCard(MusicKind.Track, "Açaí", "Outra Pessoa", null, null, null)))
        assertEquals(false, vm.play(MusicCard(MusicKind.Track, "Outra", null, null, null, "https://x")))
    }

    @Test
    fun cartaoDaMusicaLevaSoInformacao() = runTest(dispatcher) {
        val card = buildTrackCard(acai, NoDeezer)
        assertEquals("Açaí", card.title)
        // Sem resultado no Deezer: o link é a busca no YouTube Music.
        assertTrue(card.link!!.startsWith("https://music.youtube.com/search"))
    }

    @Test
    fun perfilAmizadeEBloqueio() = runTest(dispatcher) {
        val social = FakeSocial()
        val vm = FriendProfileViewModel("a1", social)
        assertEquals(Relation.None, (vm.state.value as ProfileUiState.Ready).profile.relation)
        vm.addFriend()
        assertEquals(Relation.Sent, (vm.state.value as ProfileUiState.Ready).profile.relation)
        vm.removeFriend()
        assertEquals(Relation.None, (vm.state.value as ProfileUiState.Ready).profile.relation)
        social.relation = Relation.Received
        vm.load()
        vm.addFriend()
        assertEquals(Relation.Friend, (vm.state.value as ProfileUiState.Ready).profile.relation)
        vm.toggleBlock()
        assertTrue((vm.state.value as ProfileUiState.Ready).profile.blockedByMe)
    }

    @Test
    fun reacaoSoComConexao() = runTest(dispatcher) {
        val social = FakeSocial()
        val vm = FriendProfileViewModel("a1", social)
        assertEquals(false, vm.react("🔥"))
        social.live.value = SocialLive(LinkStatus.Online)
        assertEquals(true, vm.react("🔥"))
        assertEquals(listOf("a1" to "🔥", "a1" to "🔥"), social.reacted)
    }

    private fun listener(id: String, song: String) = FriendProfile(
        user = SocialUser(id, id, id),
        sharing = true,
        now = TrackInfo(song, null, null),
        updatedMs = null,
        recent = emptyList(),
        blockedByMe = false,
        relation = Relation.Friend,
    )

    @Test
    fun listaAoVivoQuandoConectado() = runTest {
        val social = FakeSocial().apply { account.value = SocialAccount("me", "eu", "Eu", "t", sharing = true) }
        val vm = SocialHomeViewModel(social)
        val seen = mutableListOf<List<FriendProfile>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.friends.collect { seen += it } }

        // Sem conexão: consulta pelo HTTP.
        social.fetchedPeople = listOf(listener("b", "Antiga"))
        vm.refresh()
        assertEquals(1, social.syncs)
        assertEquals("Antiga", vm.friends.value.single().now?.title)

        // Conectado: a lista vem ao vivo.
        social.live.value = SocialLive(LinkStatus.Online, listOf(listener("b", "Ao vivo")))
        assertEquals("Ao vivo", vm.friends.value.single().now?.title)
    }

    @Test
    fun descansandoMostraAteQuandoENaoInsiste() = runTest {
        val social = FakeSocial().apply { account.value = SocialAccount("me", "eu", "Eu", "t", sharing = false) }
        val vm = SocialHomeViewModel(social)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.restingUntil.collect {} }
        social.live.value = SocialLive(LinkStatus.Resting(1_000L))
        assertEquals(1_000L, vm.restingUntil.value)
        vm.refresh()
        assertEquals(0, social.syncs)
        social.live.value = SocialLive(LinkStatus.Online)
        assertNull(vm.restingUntil.value)
    }
}
