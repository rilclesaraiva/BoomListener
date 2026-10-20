// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.social

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boomlistener.core.model.Track
import com.boomlistener.core.player.PlaybackController
import com.boomlistener.data.social.FriendProfile
import com.boomlistener.data.social.FriendsState
import com.boomlistener.data.social.HandleCheck
import com.boomlistener.data.social.LinkStatus
import com.boomlistener.data.social.MusicCard
import com.boomlistener.data.social.Recommendation
import com.boomlistener.data.social.Relation
import com.boomlistener.data.social.SocialAccount
import com.boomlistener.data.social.SocialRepository
import com.boomlistener.data.social.SocialResult
import com.boomlistener.data.social.sortFriends
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Aba Amigos: "O que seus amigos estão ouvindo", pedidos de amizade e as
 * músicas que os amigos recomendaram. Não existe chat.
 */
internal class SocialHomeViewModel(
    private val social: SocialRepository,
    /** As músicas do aparelho (para tocar uma recomendação que você já tem). */
    private val library: suspend () -> List<Track> = { emptyList() },
    private val player: PlaybackController? = null,
    private val remoteImage: suspend (String, Int) -> Bitmap? = { _, _ -> null },
) : ViewModel() {

    val account: StateFlow<SocialAccount?> = social.account

    /** Seus amigos, quem está ouvindo agora primeiro. */
    val friends: StateFlow<List<FriendProfile>> = combine(social.friends, social.live) { state, live ->
        sortFriends(live.people ?: state.friends)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Pedidos de amizade recebidos e enviados. */
    val requests: StateFlow<FriendsState> = social.friends

    val recommendations: StateFlow<List<Recommendation>> = social.recommendations

    private val _offline = MutableStateFlow(false)
    /** A última atualização não conseguiu falar com o servidor (e não há conexão ao vivo). */
    val offline: StateFlow<Boolean> = combine(_offline, social.live) { failed, live -> failed && live.status != LinkStatus.Online }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Até quando o social descansa (cota diária do plano gratuito esgotada); null = funcionando. */
    val restingUntil: StateFlow<Long?> = social.live
        .map { (it.status as? LinkStatus.Resting)?.untilMs }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val tracks = MutableStateFlow<List<Track>>(emptyList())

    init {
        viewModelScope.launch { tracks.value = library() }
    }

    /** Busca amigos e pedidos (ao abrir a aba). Descansando, não insiste. */
    suspend fun refresh() {
        if (social.account.value == null) return
        if (social.live.value.status is LinkStatus.Resting) return
        _offline.value = social.refreshFriends() is SocialResult.Offline
    }

    suspend fun checkHandle(handle: String): HandleCheck = social.checkHandle(handle)

    /** Cria o perfil e, se escolhido, já liga o compartilhamento. */
    suspend fun register(name: String, handle: String, share: Boolean): SocialResult<SocialAccount> {
        val result = social.register(name, handle)
        if (result is SocialResult.Ok && share) return social.updateProfile(sharing = true)
        return result
    }

    suspend fun findUser(handle: String): SocialResult<FriendProfile> = social.findByHandle(handle)

    fun addFriend(userId: String) {
        viewModelScope.launch { social.addFriend(userId) }
    }

    /** Recusar um pedido, cancelar o seu, ou desfazer a amizade. */
    fun removeFriend(userId: String) {
        viewModelScope.launch { social.removeFriend(userId) }
    }

    /** Reage ao que o amigo está ouvindo; false se não deu (sem conexão ao vivo). */
    suspend fun react(friend: FriendProfile, emoji: String): Boolean =
        social.react(friend.user.id, emoji, friend.now?.title)

    fun markRecommendationsSeen() {
        viewModelScope.launch { social.markRecommendationsSeen() }
    }

    fun removeRecommendation(id: String) {
        viewModelScope.launch { social.removeRecommendation(id) }
    }

    /** A música recomendada que você já tem no aparelho, ou null. */
    fun localMatch(card: MusicCard): Track? = findLocalMatch(card, tracks.value)

    /** Toca a recomendação, se você tiver a música. false = não tem (abre o link). */
    fun play(card: MusicCard): Boolean {
        val local = localMatch(card) ?: return false
        player?.play(listOf(local), 0) ?: return false
        return true
    }

    suspend fun image(url: String, sizePx: Int): Bitmap? = remoteImage(url, sizePx)
}

internal sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data object NotFound : ProfileUiState
    data object Offline : ProfileUiState
    data class Ready(val profile: FriendProfile) : ProfileUiState
}

/** O perfil de outra pessoa: redes, o que ela está ouvindo (se é amiga e compartilha) e as ações. */
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

    /** Pede amizade, ou aceita o pedido da pessoa. */
    fun addFriend() = changeFriend { social.addFriend(userId) }

    /** Cancela o pedido, recusa o dela ou desfaz a amizade. */
    fun removeFriend() = changeFriend { social.removeFriend(userId) }

    private fun changeFriend(action: suspend () -> SocialResult<Relation>) {
        val profile = (_state.value as? ProfileUiState.Ready)?.profile ?: return
        viewModelScope.launch {
            val result = action()
            if (result is SocialResult.Ok) {
                // Virou (ou deixou de ser) amigo: busca de novo (número e "ouvindo" mudam).
                _state.value = ProfileUiState.Ready(profile.copy(relation = result.value))
                if (result.value == Relation.Friend || profile.relation == Relation.Friend) load()
            }
        }
    }

    suspend fun react(emoji: String): Boolean {
        val profile = (_state.value as? ProfileUiState.Ready)?.profile ?: return false
        return social.react(userId, emoji, profile.now?.title)
    }

    fun toggleBlock() {
        val profile = (_state.value as? ProfileUiState.Ready)?.profile ?: return
        viewModelScope.launch {
            val result = if (profile.blockedByMe) social.unblock(userId) else social.block(userId)
            if (result is SocialResult.Ok) load()
        }
    }
}
