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
    /** Suas redes públicas (só o @ de cada uma). */
    val links: Map<SocialNetwork, String> = emptyMap(),
    /** Seus números (WhatsApp, Telegram), com código do país; só vão aos amigos se [shareNumbers]. */
    val numbers: Map<NumberNetwork, String> = emptyMap(),
    val shareNumbers: Boolean = false,
)

data class SocialUser(val id: String, val handle: String, val name: String)

/** Só informação da música (nunca o arquivo). */
data class TrackInfo(
    val title: String,
    val artist: String?,
    val album: String?,
    /** Capa (imagem do Deezer), para os amigos verem; null se não achou. */
    val coverUrl: String? = null,
)

data class ListenedTrack(val track: TrackInfo, val atMs: Long)

/** O que você é de alguém: você mesmo, amigo, pedido seu, pedido dele, ou nada. */
enum class Relation { Self, Friend, Sent, Received, None }

/**
 * O que você pode ver de outra pessoa. Redes públicas ([links]) para todos;
 * [numbers], [now] e [recent] só para amigos (e o "ouvindo" só se ela compartilha).
 */
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
    val links: Map<SocialNetwork, String> = emptyMap(),
    val numbers: Map<NumberNetwork, String> = emptyMap(),
    val relation: Relation = Relation.None,
) {
    /** O que ouviu por último (o "agora", ou o recente mais novo). */
    val lastTrack: TrackInfo? get() = now ?: recent.firstOrNull()?.track
}

/** Seus amigos (com o que estão ouvindo) e os pedidos de amizade. */
data class FriendsState(
    val friends: List<FriendProfile> = emptyList(),
    val incoming: List<SocialUser> = emptyList(),
    val outgoing: List<SocialUser> = emptyList(),
)

/** Foto de perfil (quadrada) ou capa (faixa larga no topo do perfil). */
enum class ProfileImage(val path: String, val width: Int, val height: Int) {
    // 512 px e 1080 px, em WebP (bem menor que o JPEG).
    Avatar("avatar", 512, 512),
    Banner("banner", 1080, 405),
}

enum class MusicKind { Track, Album, Artist }

/** Cartão de música de uma recomendação: só informação, nunca o arquivo. */
data class MusicCard(
    val kind: MusicKind,
    val title: String,
    val artist: String?,
    val album: String?,
    val coverUrl: String?,
    val link: String?,
)

/** Uma música que um amigo recomendou para você. */
data class Recommendation(
    val id: String,
    val from: SocialUser?,
    val card: MusicCard,
    val atMs: Long,
    val seen: Boolean = false,
)

/** Um amigo reagiu ao que você está ouvindo (só ao vivo; nada fica guardado). */
data class Reaction(val from: SocialUser, val emoji: String, val title: String?, val atMs: Long)

/** As reações possíveis: sem texto livre, nada para moderar. */
val REACTION_EMOJIS = listOf("🔥", "❤️", "😂", "😮", "👏", "🎧")

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
 * O que chega ao vivo pela conexão: o estado dela e seus amigos com o que
 * estão ouvindo ([people] null = ainda não veio nada desde que conectou).
 */
data class SocialLive(val status: LinkStatus = LinkStatus.Offline, val people: List<FriendProfile>? = null)
