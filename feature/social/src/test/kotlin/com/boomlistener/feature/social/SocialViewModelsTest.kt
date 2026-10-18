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
import com.boomlistener.data.social.ChatMessage
import com.boomlistener.data.social.Conversation
import com.boomlistener.data.social.FriendProfile
import com.boomlistener.data.social.HandleCheck
import com.boomlistener.data.social.MessageBody
import com.boomlistener.data.social.MusicKind
import com.boomlistener.data.social.SocialAccount
import com.boomlistener.data.social.SocialRepository
import com.boomlistener.data.social.SocialResult
import com.boomlistener.data.social.SocialUser
import com.boomlistener.data.social.TrackInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
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
    val sent = mutableListOf<Pair<String, MessageBody>>()
    var blocked = false
    override val conversations: Flow<List<Conversation>> = flowOf(emptyList())
    override fun messages(conversationId: String): Flow<List<ChatMessage>> = flowOf(emptyList())
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
    override suspend fun deleteAccount() = SocialResult.Ok(Unit)
    override suspend fun findByHandle(handle: String): SocialResult<FriendProfile> = SocialResult.NotFound
    override suspend fun profile(userId: String): SocialResult<FriendProfile> =
        SocialResult.Ok(FriendProfile(ana, sharing = true, now = TrackInfo("Oceano", "Djavan", null), updatedMs = 1, recent = emptyList(), blockedByMe = blocked))
    override suspend fun activity(): SocialResult<List<FriendProfile>> = SocialResult.Ok(emptyList())
    override suspend fun openDirect(user: SocialUser) = "dm:" + user.id
    override suspend fun send(conversationId: String, body: MessageBody): SocialResult<Unit> {
        sent += conversationId to body
        return SocialResult.Ok(Unit)
    }
    override suspend fun sync(): SocialResult<Int> = SocialResult.Ok(0)
    override suspend fun createGroup(name: String): SocialResult<Conversation> = SocialResult.Invalid
    override suspend fun joinGroup(code: String): SocialResult<Conversation> = SocialResult.NotFound
    override suspend fun groupMembers(conversationId: String): SocialResult<List<SocialUser>> = SocialResult.Ok(listOf(ana))
    override suspend fun leaveGroup(conversationId: String) = SocialResult.Ok(Unit)
    override suspend fun block(userId: String): SocialResult<Unit> { blocked = true; return SocialResult.Ok(Unit) }
    override suspend fun unblock(userId: String): SocialResult<Unit> { blocked = false; return SocialResult.Ok(Unit) }
    override fun setOpenConversation(conversationId: String?) = Unit
    override suspend fun markRead(conversationId: String) = Unit
    override suspend fun deleteConversation(conversationId: String) = Unit
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
    fun recomendarMandaSoInformacaoETocarSeTiver() = runTest(dispatcher) {
        val social = FakeSocial()
        val player = FakePlayer()
        val vm = ChatViewModel("dm:a1", social, FakeLibrary(), NoDeezer, player)

        vm.sendTrack(acai, note = " ouve essa ")
        val card = social.sent.single().second as MessageBody.Music
        assertEquals("Açaí", card.title)
        assertEquals("ouve essa", card.note)
        // Sem resultado no Deezer: o link é a busca no YouTube Music.
        assertTrue(card.link!!.startsWith("https://music.youtube.com/search"))

        assertTrue(vm.play(MessageBody.Music(MusicKind.Track, "ACAI", "djavan", null, null, null, null)))
        assertEquals(listOf(acai), player.played)
        assertNull(vm.localMatch(MessageBody.Music(MusicKind.Track, "Açaí", "Outra Pessoa", null, null, null, null)))
    }

    @Test
    fun textoVazioNaoVai() = runTest(dispatcher) {
        val social = FakeSocial()
        val vm = ChatViewModel("dm:a1", social, FakeLibrary(), NoDeezer, FakePlayer())
        vm.sendText("   ")
        vm.sendText(" oi ")
        assertEquals(listOf("dm:a1" to MessageBody.Text("oi")), social.sent)
    }

    @Test
    fun perfilBloquearEDesbloquear() = runTest(dispatcher) {
        val social = FakeSocial()
        val vm = FriendProfileViewModel("a1", social)
        assertEquals("Oceano", (vm.state.value as ProfileUiState.Ready).profile.now?.title)
        vm.toggleBlock()
        assertTrue((vm.state.value as ProfileUiState.Ready).profile.blockedByMe)
        assertEquals("dm:a1", vm.openChat())
    }
}
