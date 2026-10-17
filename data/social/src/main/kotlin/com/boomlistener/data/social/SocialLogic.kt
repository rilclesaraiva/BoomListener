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
