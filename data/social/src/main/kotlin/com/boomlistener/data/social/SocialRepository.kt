// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.social

import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Amigos: perfil (@, foto, redes), amizades (pedido + aceite), o "ouvindo
 * agora" dos amigos, recomendações de música e reações com emoji. Não existe
 * chat: a conversa acontece nas redes de cada um (ver docs/PLANO-SOCIAL.md).
 * Nunca passa áudio.
 */
interface SocialRepository {
    /** null = ainda não criou o perfil. */
    val account: StateFlow<SocialAccount?>

    suspend fun checkHandle(handle: String): HandleCheck
    suspend fun register(name: String, handle: String): SocialResult<SocialAccount>
    suspend fun updateProfile(name: String? = null, sharing: Boolean? = null): SocialResult<SocialAccount>

    /** Muda a descrição do perfil (até 160 letras). */
    suspend fun updateBio(bio: String): SocialResult<SocialAccount> = SocialResult.Invalid

    /** Cor do perfil: "#RRGGBB", ou "" para nenhuma. */
    suspend fun updateColor(color: String): SocialResult<SocialAccount> = SocialResult.Invalid

    /**
     * Redes do perfil. [links]: só o @ de cada rede pública (vazio tira).
     * [numbers]: números com código do país; só vão para o servidor (e para os
     * amigos) com [shareNumbers] ligado. Desligar apaga do servidor.
     */
    suspend fun updateLinks(
        links: Map<SocialNetwork, String>,
        numbers: Map<NumberNetwork, String>,
        shareNumbers: Boolean,
    ): SocialResult<SocialAccount> = SocialResult.Invalid

    /**
     * Troca a foto ou a capa por [uri] (o content:// do seletor de fotos do
     * Android); null tira a imagem. O app reduz e recorta antes de enviar.
     */
    suspend fun setProfileImage(kind: ProfileImage, uri: String?): SocialResult<SocialAccount> = SocialResult.Invalid

    /** A foto ou capa de alguém (guardada no aparelho por versão); null se não houver. */
    suspend fun profileImage(userId: String, kind: ProfileImage, version: Long): ByteArray? = null

    /** Apaga o perfil no servidor e tudo o que está no aparelho. */
    suspend fun deleteAccount(): SocialResult<Unit>

    suspend fun findByHandle(handle: String): SocialResult<FriendProfile>
    suspend fun profile(userId: String): SocialResult<FriendProfile>

    /**
     * Nome, foto, capa, cor e redes de alguém, guardados no aparelho: só
     * baixa de novo quando a versão do perfil muda (para listas de amigos).
     * null sem internet e sem nada guardado.
     */
    suspend fun profileCard(userId: String): FriendProfile? = (profile(userId) as? SocialResult.Ok)?.value

    // ---- Amizades ----

    /** Seus amigos (com o que estão ouvindo) e os pedidos de amizade. */
    val friends: StateFlow<FriendsState> get() = NO_FRIENDS

    suspend fun refreshFriends(): SocialResult<FriendsState> = SocialResult.Invalid

    /** Pede amizade (ou aceita, se a pessoa já tinha pedido). Devolve como ficou. */
    suspend fun addFriend(userId: String): SocialResult<Relation> = SocialResult.Invalid

    /** Desfaz a amizade, cancela o seu pedido ou recusa o dela. */
    suspend fun removeFriend(userId: String): SocialResult<Relation> = SocialResult.Invalid

    /** Seus amigos com o que estão ouvindo (o mesmo de [friends], buscado agora). */
    suspend fun activity(): SocialResult<List<FriendProfile>>

    // ---- Recomendações e reações ----

    /** Músicas que os amigos recomendaram (a mais nova primeiro; guardadas no aparelho). */
    val recommendations: StateFlow<List<Recommendation>> get() = NO_RECOMMENDATIONS

    /** Recomenda [card] para os amigos [to] (só a informação, nunca o arquivo). */
    suspend fun recommend(to: List<String>, card: MusicCard): SocialResult<Unit> = SocialResult.Invalid

    /** Viu a lista de recomendações (some o número na aba). */
    suspend fun markRecommendationsSeen() {}

    suspend fun removeRecommendation(id: String) {}

    /** Reações que chegam ao vivo ao que você está ouvindo. */
    val reactions: SharedFlow<Reaction> get() = NO_REACTIONS

    /** Reage com [emoji] ao que o amigo [toUserId] está ouvindo ([title]). Só ao vivo. */
    suspend fun react(toUserId: String, emoji: String, title: String?): Boolean = false

    suspend fun block(userId: String): SocialResult<Unit>
    suspend fun unblock(userId: String): SocialResult<Unit>

    /**
     * A conexão ao vivo: estado e seus amigos com o que estão ouvindo. Com
     * ela aberta, tudo chega sozinho (sem ficar perguntando ao servidor).
     */
    val live: StateFlow<SocialLive> get() = NO_LIVE

    /**
     * Pede (ou dispensa) a conexão ao vivo por um motivo: "ui" (app na tela),
     * "playing" (música tocando com o "ouvindo agora" ligado). Fica conectado
     * enquanto houver algum motivo.
     */
    fun keepConnected(reason: String, wanted: Boolean) {}

    /** "Ouvindo agora" (só vai se o compartilhamento estiver ligado). */
    suspend fun publishNowPlaying(track: TrackInfo?)

    /** Música ouvida (entra nos "recentes" que os amigos veem, se compartilhar). */
    suspend fun publishListened(track: TrackInfo)
}

private val NO_LIVE: StateFlow<SocialLive> = MutableStateFlow(SocialLive())
private val NO_FRIENDS: StateFlow<FriendsState> = MutableStateFlow(FriendsState())
private val NO_RECOMMENDATIONS: StateFlow<List<Recommendation>> = MutableStateFlow(emptyList())
private val NO_REACTIONS: SharedFlow<Reaction> = MutableSharedFlow()

/** Ponto de acesso provisório enquanto o app não tem Hilt. */
object SocialData {
    @Volatile
    private var instance: SocialRepository? = null

    fun repository(context: Context): SocialRepository =
        instance ?: synchronized(this) {
            instance ?: HttpSocialRepository(context.applicationContext).also { instance = it }
        }
}
