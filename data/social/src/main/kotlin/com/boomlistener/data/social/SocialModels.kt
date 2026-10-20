// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.social

/** O seu perfil, criado neste aparelho. [token] é o segredo que prova que é você. */
data class SocialAccount(
    val id: String,
    val handle: String,
    val name: String,
    val token: String,
    val sharing: Boolean,
    /** Descrição curta do perfil. */
    val bio: String = "",
    /** Versão da foto e da capa no servidor (0 = sem imagem). */
    val avatarVersion: Long = 0,
    val bannerVersion: Long = 0,
    /** Cor do perfil, "#RRGGBB" ("" = nenhuma). */
    val color: String = "",
)

data class SocialUser(val id: String, val handle: String, val name: String)

/** Só informação da música (nunca o arquivo). */
data class TrackInfo(val title: String, val artist: String?, val album: String?)

data class ListenedTrack(val track: TrackInfo, val atMs: Long)

/** O que você pode ver de outra pessoa. [now] e [recent] só vêm se ela compartilha com você. */
data class FriendProfile(
    val user: SocialUser,
    val sharing: Boolean,
    val now: TrackInfo?,
    val updatedMs: Long?,
    val recent: List<ListenedTrack>,
    val blockedByMe: Boolean,
    val bio: String = "",
    val avatarVersion: Long = 0,
    val bannerVersion: Long = 0,
    val color: String = "",
    /** Versão do perfil (muda a cada edição): o app só baixa de novo quando muda. */
    val version: Long = 0,
)

/** Foto de perfil (quadrada) ou capa (faixa larga no topo do perfil). */
enum class ProfileImage(val path: String, val width: Int, val height: Int) {
    // Fase 2 do plano: 512 px e 1080 px, em WebP (bem menor que o JPEG).
    Avatar("avatar", 512, 512),
    Banner("banner", 1080, 405),
}

/** O conteúdo de uma mensagem: texto ou um cartão de música/álbum/artista. */
sealed interface MessageBody {
    data class Text(val text: String) : MessageBody

    data class Music(
        val kind: MusicKind,
        val title: String,
        val artist: String?,
        val album: String?,
        val coverUrl: String?,
        val link: String?,
        val note: String?,
    ) : MessageBody
}

enum class MusicKind { Track, Album, Artist }

data class ChatMessage(
    val id: String,
    val conversationId: String,
    /** null = você. */
    val sender: SocialUser?,
    val body: MessageBody,
    val sentMs: Long,
    /** Onde a mensagem está (só faz diferença nas suas). */
    val delivery: Delivery = Delivery.Sent,
) {
    /** Ainda está no seu celular, esperando a outra pessoa aparecer. */
    val pending: Boolean get() = delivery == Delivery.Waiting
}

/**
 * Onde está uma mensagem sua. Numa conversa particular ela fica no seu celular
 * ([Waiting]) até a outra pessoa aparecer; aí vai direto ([Delivered]). Se
 * demorar demais, vai para a caixa postal do servidor ([Mailbox]).
 */
enum class Delivery {
    /** Aguardando a outra pessoa aparecer (fica no seu celular). */
    Waiting,

    /** Chegou ao servidor (grupos e versões antigas). */
    Sent,

    /** Chegou no celular da outra pessoa. */
    Delivered,

    /** Na caixa postal do servidor (até 7 dias). */
    Mailbox,
}

/**
 * Uma conversa: particular ("dm:<id da pessoa>") ou grupo ("g:<id do grupo>").
 * [code] é o código de convite do grupo.
 */
data class Conversation(
    val id: String,
    val title: String,
    val isGroup: Boolean,
    val code: String?,
    val lastMessage: ChatMessage?,
    val unread: Int,
    val updatedMs: Long,
) {
    /** A outra pessoa, numa conversa particular. */
    val otherUserId: String? get() = if (id.startsWith(DM_PREFIX)) id.removePrefix(DM_PREFIX) else null
    val groupId: String? get() = if (id.startsWith(GROUP_PREFIX)) id.removePrefix(GROUP_PREFIX) else null
}

const val DM_PREFIX = "dm:"
const val GROUP_PREFIX = "g:"

sealed interface SocialResult<out T> {
    data class Ok<T>(val value: T) : SocialResult<T>
    data object NotFound : SocialResult<Nothing>
    data object HandleTaken : SocialResult<Nothing>
    data object Invalid : SocialResult<Nothing>
    /** Sem internet ou servidor fora do ar. */
    data object Offline : SocialResult<Nothing>
}

enum class HandleCheck { Available, Taken, Invalid, Offline }

/** Estado da conexão ao vivo com o servidor social. */
sealed interface LinkStatus {
    /** Sem conexão (app fechado, sem internet, ou ainda sem perfil). */
    data object Offline : LinkStatus
    data object Connecting : LinkStatus
    data object Online : LinkStatus
    /** A cota diária do plano gratuito acabou: o social volta em [untilMs] (meia-noite UTC). */
    data class Resting(val untilMs: Long) : LinkStatus
}

/**
 * O que chega ao vivo pela conexão: o estado dela e quem está ouvindo o quê
 * ([people] null = ainda não veio nada desde que conectou).
 */
data class SocialLive(val status: LinkStatus = LinkStatus.Offline, val people: List<FriendProfile>? = null)
