// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.social

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boomlistener.core.model.Track
import com.boomlistener.core.player.PlaybackController
import com.boomlistener.data.deezer.DeezerRepository
import com.boomlistener.data.library.LibraryRepository
import com.boomlistener.data.social.ChatMessage
import com.boomlistener.data.social.Conversation
import com.boomlistener.data.social.FriendProfile
import com.boomlistener.data.social.HandleCheck
import com.boomlistener.data.social.LinkStatus
import com.boomlistener.data.social.MessageBody
import com.boomlistener.data.social.SocialAccount
import com.boomlistener.data.social.SocialRepository
import com.boomlistener.data.social.SocialResult
import com.boomlistener.data.social.SocialUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Aba Amigos: perfil, quem está ouvindo o quê e a lista de conversas. */
internal class SocialHomeViewModel(
    private val social: SocialRepository,
) : ViewModel() {

    val account: StateFlow<SocialAccount?> = social.account

    val conversations: StateFlow<List<Conversation>> = social.conversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _activity = MutableStateFlow<List<FriendProfile>>(emptyList())

    /** Quem está ouvindo o quê: ao vivo pela conexão, ou o da última consulta. */
    val activity: StateFlow<List<FriendProfile>> = combine(_activity, social.live) { fetched, live -> live.people ?: fetched }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _offline = MutableStateFlow(false)
    /** A última atualização não conseguiu falar com o servidor (e não há conexão ao vivo). */
    val offline: StateFlow<Boolean> = combine(_offline, social.live) { failed, live -> failed && live.status != LinkStatus.Online }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Até quando o social descansa (cota diária do plano gratuito esgotada); null = funcionando. */
    val restingUntil: StateFlow<Long?> = social.live
        .map { (it.status as? LinkStatus.Resting)?.untilMs }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Consulta o servidor só quando não há conexão ao vivo (com ela, tudo chega
     * sozinho). Descansando, não insiste.
     */
    suspend fun refresh() {
        if (social.account.value == null) return
        when (social.live.value.status) {
            LinkStatus.Online, is LinkStatus.Resting -> return
            else -> Unit
        }
        val synced = social.sync()
        val people = social.activity()
        _offline.value = synced is SocialResult.Offline
        if (people is SocialResult.Ok) _activity.value = people.value
    }

    /** Mensagens com o app fechado (UnifiedPush, ex.: ntfy). */
    val pushEnabled: StateFlow<Boolean> = social.pushEnabled

    fun pushDistributors(): List<String> = social.pushDistributors()

    fun enablePush(distributor: String) = social.enablePush(distributor)

    fun disablePush() = social.disablePush()

    suspend fun checkHandle(handle: String): HandleCheck = social.checkHandle(handle)

    /** Cria o perfil e, se escolhido, já liga o compartilhamento. */
    suspend fun register(name: String, handle: String, share: Boolean): SocialResult<SocialAccount> {
        val result = social.register(name, handle)
        if (result is SocialResult.Ok && share) return social.updateProfile(sharing = true)
        return result
    }

    suspend fun findUser(handle: String): SocialResult<FriendProfile> = social.findByHandle(handle)

    suspend fun openDirect(user: SocialUser): String = social.openDirect(user)

    suspend fun createGroup(name: String): SocialResult<Conversation> = social.createGroup(name)

    suspend fun joinGroup(code: String): SocialResult<Conversation> = social.joinGroup(code)
}

/** Uma conversa aberta (particular ou grupo). */
internal class ChatViewModel(
    val conversationId: String,
    private val social: SocialRepository,
    private val libraryRepository: LibraryRepository,
    private val deezer: DeezerRepository,
    private val player: PlaybackController,
) : ViewModel() {

    val conversation: StateFlow<Conversation?> = social.conversations
        .map { list -> list.firstOrNull { it.id == conversationId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val messages: StateFlow<List<ChatMessage>> = social.messages(conversationId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _members = MutableStateFlow<List<SocialUser>>(emptyList())
    val members: StateFlow<List<SocialUser>> = _members.asStateFlow()

    private val _library = MutableStateFlow<List<Track>>(emptyList())
    /** As músicas do aparelho (para tocar um cartão que você já tem e para recomendar). */
    val localTracks: StateFlow<List<Track>> = _library.asStateFlow()

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending.asStateFlow()

    init {
        viewModelScope.launch { _library.value = libraryRepository.getTracks() }
        if (conversationId.startsWith("g:")) refreshMembers()
    }

    /** Entrou na conversa: zera as não lidas e não acumula enquanto estiver aberta. */
    fun opened() {
        social.setOpenConversation(conversationId)
        viewModelScope.launch { social.markRead(conversationId) }
    }

    fun closed() {
        social.setOpenConversation(null)
    }

    /** Sem conexão ao vivo, busca as mensagens novas (com ela, chegam sozinhas). */
    suspend fun sync() {
        if (social.live.value.status == LinkStatus.Online) return
        social.sync()
        social.markRead(conversationId)
    }

    fun sendText(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch { social.send(conversationId, MessageBody.Text(clean)) }
    }

    /** Recomenda uma música do aparelho (vai só a informação, nunca o arquivo). */
    fun sendTrack(track: Track, note: String? = null) {
        viewModelScope.launch {
            _sending.value = true
            social.send(conversationId, buildTrackCard(track, deezer, note))
            _sending.value = false
        }
    }

    /** Toca a música do cartão, se você também tiver. */
    fun play(card: MessageBody.Music): Boolean {
        val local = findLocalMatch(card, _library.value) ?: return false
        player.play(listOf(local), 0)
        return true
    }

    fun localMatch(card: MessageBody.Music): Track? = findLocalMatch(card, _library.value)

    fun refreshMembers() {
        viewModelScope.launch {
            val result = social.groupMembers(conversationId)
            if (result is SocialResult.Ok) _members.value = result.value
        }
    }

    suspend fun leave(): Boolean = social.leaveGroup(conversationId) is SocialResult.Ok

    suspend fun delete() = social.deleteConversation(conversationId)

    suspend fun image(url: String, sizePx: Int): Bitmap? = deezer.image(url, sizePx)

    suspend fun artwork(uri: String, sizePx: Int): Bitmap? = libraryRepository.loadArtwork(uri, sizePx)
}

internal sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data object NotFound : ProfileUiState
    data object Offline : ProfileUiState
    data class Ready(val profile: FriendProfile) : ProfileUiState
}

/** O perfil de outra pessoa: o que ela está ouvindo (se compartilha) e as ações. */
internal class FriendProfileViewModel(
    private val userId: String,
    private val social: SocialRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = when (val result = social.profile(userId)) {
                is SocialResult.Ok -> ProfileUiState.Ready(result.value)
                SocialResult.Offline -> ProfileUiState.Offline
                else -> ProfileUiState.NotFound
            }
        }
    }

    suspend fun openChat(): String? {
        val profile = (_state.value as? ProfileUiState.Ready)?.profile ?: return null
        return social.openDirect(profile.user)
    }

    fun toggleBlock() {
        val profile = (_state.value as? ProfileUiState.Ready)?.profile ?: return
        viewModelScope.launch {
            val result = if (profile.blockedByMe) social.unblock(userId) else social.block(userId)
            if (result is SocialResult.Ok) {
                _state.value = ProfileUiState.Ready(profile.copy(blockedByMe = !profile.blockedByMe))
            }
        }
    }
}
