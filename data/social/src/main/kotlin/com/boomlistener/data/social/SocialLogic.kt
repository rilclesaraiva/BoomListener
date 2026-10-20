// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.social

/*
 * Regras puras do social (testadas): validar o @, cor do perfil, reconexão,
 * lista de amigos ao vivo e recomendações guardadas no aparelho.
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

/** Atualiza um amigo na lista (ou tira, se deixou de ser amigo), mantendo a posição. */
internal fun withPerson(people: List<FriendProfile>, profile: FriendProfile): List<FriendProfile> {
    if (profile.relation != Relation.Friend) return people.filterNot { it.user.id == profile.user.id }
    val index = people.indexOfFirst { it.user.id == profile.user.id }
    return if (index < 0) people + profile else people.toMutableList().also { it[index] = profile }
}

/**
 * Amigos na aba: quem está ouvindo agora primeiro, depois quem ouviu algo há
 * menos tempo, depois quem não compartilha (por nome).
 */
fun sortFriends(friends: List<FriendProfile>): List<FriendProfile> =
    friends.sortedWith(
        compareByDescending<FriendProfile> { it.now != null }
            .thenByDescending { it.recent.firstOrNull()?.atMs ?: it.updatedMs ?: 0L }
            .thenBy { it.user.name.lowercase() },
    )

/** Recomendações guardadas: até 50, de no máximo 30 dias, sem repetir, a mais nova primeiro. */
internal const val MAX_RECOMMENDATIONS = 50
internal const val RECOMMENDATION_DAYS_MS = 30L * 86_400_000L

internal fun mergeRecommendations(stored: List<Recommendation>, incoming: List<Recommendation>, nowMs: Long): List<Recommendation> {
    val known = stored.associateBy { it.id }
    val fresh = incoming.filter { it.id !in known }
    return (stored + fresh)
        .filter { nowMs - it.atMs <= RECOMMENDATION_DAYS_MS }
        .sortedByDescending { it.atMs }
        .take(MAX_RECOMMENDATIONS)
}
