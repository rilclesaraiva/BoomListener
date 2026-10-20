// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.social

/*
 * Regras puras do chat (testadas): validar o @, juntar mensagens que chegam
 * às conversas guardadas no aparelho.
 */

private val HANDLE = Regex("^[a-z0-9_.]{3,20}$")

/** "@Ana.Clara " → "ana.clara". */
fun normalizeHandle(raw: String): String = raw.trim().removePrefix("@").lowercase()

fun isValidHandle(raw: String): Boolean = HANDLE.matches(normalizeHandle(raw))

/** Sugestão de @ a partir do nome: "Rilcle Saraiva" → "rilcle_saraiva". */
fun suggestHandle(name: String): String =
    java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
        .take(20)
        .padEnd(3, '_')

/** Mensagens novas de uma conversa, no estado guardado. */
internal data class StoredConversation(
    val conversation: Conversation,
    val messages: List<ChatMessage>,
)

/** Máximo de mensagens guardadas por conversa (as mais antigas saem). */
internal const val MAX_STORED_MESSAGES = 3_000

/**
 * Junta [incoming] (do servidor) ao que está guardado. Cria a conversa se for
 * nova (título = nome de quem mandou, numa particular), soma as não lidas
 * (menos na conversa que está aberta, [openConversationId]) e ignora repetidas.
 */
internal fun mergeIncoming(
    stored: Map<String, StoredConversation>,
    incoming: List<ChatMessage>,
    openConversationId: String?,
): Map<String, StoredConversation> {
    val result = stored.toMutableMap()
    for ((conversationId, batch) in incoming.groupBy { it.conversationId }) {
        val current = result[conversationId]
        val known = current?.messages.orEmpty().mapTo(HashSet()) { it.id }
        val fresh = batch.filter { it.id !in known }.sortedBy { it.sentMs }
        if (fresh.isEmpty() && current != null) continue
        val messages = (current?.messages.orEmpty() + fresh).sortedBy { it.sentMs }.takeLast(MAX_STORED_MESSAGES)
        val isGroup = conversationId.startsWith(GROUP_PREFIX)
        val base = current?.conversation ?: Conversation(
            id = conversationId,
            title = if (isGroup) "" else fresh.firstOrNull()?.sender?.name.orEmpty(),
            isGroup = isGroup,
            code = null,
            lastMessage = null,
            unread = 0,
            updatedMs = 0,
        )
        // Numa particular, o título acompanha o nome atual da pessoa.
        val title = if (!isGroup) fresh.lastOrNull()?.sender?.name ?: base.title else base.title
        result[conversationId] = StoredConversation(
            conversation = base.copy(
                title = title,
                lastMessage = messages.lastOrNull(),
                unread = if (conversationId == openConversationId) 0 else base.unread + fresh.size,
                updatedMs = maxOf(base.updatedMs, messages.lastOrNull()?.sentMs ?: 0),
            ),
            messages = messages,
        )
    }
    return result
}

/** Conversas na lista: a com mensagem mais recente primeiro. */
internal fun sortConversations(list: Collection<Conversation>): List<Conversation> =
    list.sortedByDescending { it.updatedMs }

private val PROFILE_COLOR = Regex("^#?([0-9a-fA-F]{6})$")

/** "#ff5500", "FF5500" → "#FF5500"; vazio → "" (sem cor); inválido → null. */
fun cleanProfileColor(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return ""
    return PROFILE_COLOR.find(trimmed)?.let { "#" + it.groupValues[1].uppercase() }
}

/**
 * Espera antes de tentar conectar de novo: uma tentativa rápida (falha
 * passageira de rede) e depois 1, 2, 4, 8 minutos, até 15, com um sorteio de
 * ±20% para os celulares não voltarem todos juntos. [random] vai de 0 a 1.
 */
internal fun reconnectDelayMs(attempt: Int, random: Double): Long {
    val base = when {
        attempt <= 1 -> 3_000L
        else -> minOf(60_000L shl (attempt - 2).coerceAtMost(4), 15 * 60_000L)
    }
    val jitter = 0.8 + 0.4 * random.coerceIn(0.0, 1.0)
    return (base * jitter).toLong()
}

/** Meia-noite UTC seguinte, quando as cotas do plano gratuito zeram. */
internal fun nextUtcMidnight(nowMs: Long): Long {
    val day = 86_400_000L
    return (nowMs / day) * day + day
}

/**
 * A resposta diz que o social está "descansando" (cota do dia esgotada)? Devolve
 * até quando, ou null. O nosso servidor responde 503 com "resting"; o limite
 * de requisições da própria Cloudflare (erro 1027) vem como 429 sem o nosso JSON.
 */
internal fun restingUntil(code: Int, error: String?, until: Long, nowMs: Long): Long? = when {
    code == 503 && error == "resting" -> until.takeIf { it > nowMs } ?: nextUtcMidnight(nowMs)
    code == 429 && error == null -> nextUtcMidnight(nowMs)
    else -> null
}

/** Atualiza (ou tira) uma pessoa da lista "ouvindo agora". */
internal fun withPerson(people: List<FriendProfile>, profile: FriendProfile): List<FriendProfile> {
    if (!profile.sharing) return people.filterNot { it.user.id == profile.user.id }
    val index = people.indexOfFirst { it.user.id == profile.user.id }
    return if (index < 0) people + profile else people.toMutableList().also { it[index] = profile }
}
