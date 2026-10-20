// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.social

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
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

    /**
     * Nome, foto, capa, cor e descrição de alguém, guardados no aparelho: só
     * baixa de novo quando a versão do perfil muda (para listas de amigos).
     * null sem internet e sem nada guardado.
     */
    suspend fun profileCard(userId: String): FriendProfile? = (profile(userId) as? SocialResult.Ok)?.value

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

    /**
     * A conexão ao vivo: estado e quem está ouvindo o quê. Com ela aberta,
     * mensagens chegam sozinhas (sem ficar perguntando ao servidor).
     */
    val live: StateFlow<SocialLive> get() = NO_LIVE

    /**
     * Pede (ou dispensa) a conexão ao vivo por um motivo: "ui" (app na tela),
     * "playing" (música tocando com o "ouvindo agora" ligado). Fica conectado
     * enquanto houver algum motivo.
     */
    fun keepConnected(reason: String, wanted: Boolean) {}

    /** Mensagens novas que acabaram de chegar (para a notificação, com o app fechado). */
    val incoming: SharedFlow<ChatMessage> get() = NO_INCOMING

    /**
     * Mensagens com o app fechado, pelo UnifiedPush (um app distribuidor como o
     * ntfy, gratuito e sem Google). O aviso só diz "acorde"; o conteúdo vem
     * pela conexão.
     */
    val pushEnabled: StateFlow<Boolean> get() = NO_PUSH

    /** Os apps distribuidores instalados (nomes de pacote). Vazio = precisa instalar um (ntfy). */
    fun pushDistributors(): List<String> = emptyList()

    fun enablePush(distributor: String) {}

    fun disablePush() {}

    /** "Ouvindo agora" (só vai se o compartilhamento estiver ligado). */
    suspend fun publishNowPlaying(track: TrackInfo?)

    /** Música ouvida (entra nos "recentes" que os amigos veem, se compartilhar). */
    suspend fun publishListened(track: TrackInfo)
}

private val NO_LIVE: StateFlow<SocialLive> = kotlinx.coroutines.flow.MutableStateFlow(SocialLive())
private val NO_INCOMING: SharedFlow<ChatMessage> = kotlinx.coroutines.flow.MutableSharedFlow()
private val NO_PUSH: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(false)

/** Ponto de acesso provisório enquanto o app não tem Hilt. */
object SocialData {
    @Volatile
    private var instance: SocialRepository? = null

    fun repository(context: Context): SocialRepository =
        instance ?: synchronized(this) {
            instance ?: HttpSocialRepository(context.applicationContext).also { instance = it }
        }
}
