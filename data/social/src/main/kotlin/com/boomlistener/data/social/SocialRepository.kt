// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.social

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Amigos: perfil (@), conversas e grupos, cartões de música e o "ouvindo
 * agora". Nunca passa áudio. As conversas ficam guardadas só no aparelho; o
 * servidor (social/worker.js) só entrega.
 */
interface SocialRepository {
    /** null = ainda não criou o perfil. */
    val account: StateFlow<SocialAccount?>

    /** Mensagem mais recente primeiro. */
    val conversations: Flow<List<Conversation>>

    fun messages(conversationId: String): Flow<List<ChatMessage>>

    suspend fun checkHandle(handle: String): HandleCheck
    suspend fun register(name: String, handle: String): SocialResult<SocialAccount>
    suspend fun updateProfile(name: String? = null, sharing: Boolean? = null): SocialResult<SocialAccount>

    /** Muda a descrição do perfil (até 160 letras). */
    suspend fun updateBio(bio: String): SocialResult<SocialAccount> = SocialResult.Invalid

    /** Cor do perfil: "#RRGGBB", ou "" para nenhuma. */
    suspend fun updateColor(color: String): SocialResult<SocialAccount> = SocialResult.Invalid

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

    /** Quem conversa com você (ou divide grupo) e compartilha o que ouve. */
    suspend fun activity(): SocialResult<List<FriendProfile>>

    /** Abre (ou cria no aparelho) a conversa particular com [user]. Devolve o id. */
    suspend fun openDirect(user: SocialUser): String

    suspend fun send(conversationId: String, body: MessageBody): SocialResult<Unit>

    /** Busca mensagens novas no servidor e reenvia as que ficaram pendentes. */
    suspend fun sync(): SocialResult<Int>

    suspend fun createGroup(name: String): SocialResult<Conversation>
    suspend fun joinGroup(code: String): SocialResult<Conversation>
    suspend fun groupMembers(conversationId: String): SocialResult<List<SocialUser>>
    suspend fun leaveGroup(conversationId: String): SocialResult<Unit>

    suspend fun block(userId: String): SocialResult<Unit>
    suspend fun unblock(userId: String): SocialResult<Unit>

    /** A conversa aberta na tela (não acumula "não lidas"); null ao sair. */
    fun setOpenConversation(conversationId: String?)
    suspend fun markRead(conversationId: String)
    suspend fun deleteConversation(conversationId: String)

    /** "Ouvindo agora" (só vai se o compartilhamento estiver ligado). */
    suspend fun publishNowPlaying(track: TrackInfo?)

    /** Música ouvida (entra nos "recentes" que os amigos veem, se compartilhar). */
    suspend fun publishListened(track: TrackInfo)
}

/** Ponto de acesso provisório enquanto o app não tem Hilt. */
object SocialData {
    @Volatile
    private var instance: SocialRepository? = null

    fun repository(context: Context): SocialRepository =
        instance ?: synchronized(this) {
            instance ?: HttpSocialRepository(context.applicationContext).also { instance = it }
        }
}
