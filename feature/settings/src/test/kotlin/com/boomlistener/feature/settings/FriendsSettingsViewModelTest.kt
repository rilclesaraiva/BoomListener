// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.settings

import com.boomlistener.data.social.ChatMessage
import com.boomlistener.data.social.Conversation
import com.boomlistener.data.social.FriendProfile
import com.boomlistener.data.social.HandleCheck
import com.boomlistener.data.social.MessageBody
import com.boomlistener.data.social.SocialAccount
import com.boomlistener.data.social.SocialRepository
import com.boomlistener.data.social.SocialResult
import com.boomlistener.data.social.SocialUser
import com.boomlistener.data.social.TrackInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeSocial(var online: Boolean = true) : SocialRepository {
    override val account = MutableStateFlow<SocialAccount?>(SocialAccount("me", "rilcle", "Rilcle", "t", sharing = false))
    var clearedNow = false
    override val conversations: Flow<List<Conversation>> = flowOf(emptyList())
    override fun messages(conversationId: String): Flow<List<ChatMessage>> = flowOf(emptyList())
    override suspend fun checkHandle(handle: String) = HandleCheck.Available
    override suspend fun register(name: String, handle: String): SocialResult<SocialAccount> = SocialResult.Invalid
    override suspend fun updateProfile(name: String?, sharing: Boolean?): SocialResult<SocialAccount> {
        if (!online) return SocialResult.Offline
        account.value = account.value!!.copy(sharing = sharing ?: account.value!!.sharing)
        return SocialResult.Ok(account.value!!)
    }
    override suspend fun deleteAccount(): SocialResult<Unit> {
        account.value = null
        return SocialResult.Ok(Unit)
    }
    override suspend fun findByHandle(handle: String): SocialResult<FriendProfile> = SocialResult.NotFound
    override suspend fun profile(userId: String): SocialResult<FriendProfile> = SocialResult.NotFound
    override suspend fun activity(): SocialResult<List<FriendProfile>> = SocialResult.Ok(emptyList())
    override suspend fun openDirect(user: SocialUser) = ""
    override suspend fun send(conversationId: String, body: MessageBody): SocialResult<Unit> = SocialResult.Ok(Unit)
    override suspend fun sync(): SocialResult<Int> = SocialResult.Ok(0)
    override suspend fun createGroup(name: String): SocialResult<Conversation> = SocialResult.Invalid
    override suspend fun joinGroup(code: String): SocialResult<Conversation> = SocialResult.Invalid
    override suspend fun groupMembers(conversationId: String): SocialResult<List<SocialUser>> = SocialResult.Ok(emptyList())
    override suspend fun leaveGroup(conversationId: String): SocialResult<Unit> = SocialResult.Ok(Unit)
    override suspend fun block(userId: String): SocialResult<Unit> = SocialResult.Ok(Unit)
    override suspend fun unblock(userId: String): SocialResult<Unit> = SocialResult.Ok(Unit)
    override fun setOpenConversation(conversationId: String?) = Unit
    override suspend fun markRead(conversationId: String) = Unit
    override suspend fun deleteConversation(conversationId: String) = Unit
    override suspend fun publishNowPlaying(track: TrackInfo?) {
        if (track == null) clearedNow = true
    }
    override suspend fun publishListened(track: TrackInfo) = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class FriendsSettingsViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun ligarEDesligarCompartilhamento() {
        val social = FakeSocial()
        val vm = FriendsSettingsViewModel(social)
        vm.setSharing(true)
        assertEquals(true, social.account.value?.sharing)
        vm.setSharing(false)
        assertEquals(false, social.account.value?.sharing)
        assertTrue(social.clearedNow)
        assertFalse(vm.failed.value)
    }

    @Test
    fun semInternetAvisa() {
        val vm = FriendsSettingsViewModel(FakeSocial(online = false))
        vm.setSharing(true)
        assertTrue(vm.failed.value)
    }

    @Test
    fun apagarPerfil() {
        val social = FakeSocial()
        FriendsSettingsViewModel(social).deleteProfile()
        assertNull(social.account.value)
    }
}
