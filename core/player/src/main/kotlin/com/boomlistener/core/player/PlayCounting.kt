// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.player

/** Uma reprodução que contou para "mais ouvidas". */
data class CountedPlay(
    val uri: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val timestampMs: Long,
    /** Gênero da tag no momento da escuta ("Rock; Pop"), se a música tinha. */
    val genre: String? = null,
)

/** Tempo realmente ouvido de uma música, medido pelo player (inclusive músicas puladas). */
data class ListenedTime(val play: CountedPlay, val ms: Long, val timestampMs: Long)

/**
 * Ganchos do player para o resto do app. O :app liga isto na inicialização
 * (o core não conhece os módulos data, pela regra de dependências).
 */
object PlaybackHooks {
    @Volatile
    var onPlayCounted: ((CountedPlay) -> Unit)? = null

    /**
     * Histórico: toda música ouvida por 30 s (inclusive as que vieram do
     * aleatório). Avisa uma vez por reprodução.
     */
    @Volatile
    var onListened: ((CountedPlay) -> Unit)? = null

    /** Resumo diário: quanto tempo se ouviu de verdade, aos poucos (a cada ~15 s, ao pausar e ao trocar de música). */
    @Volatile
    var onListenedTime: ((ListenedTime) -> Unit)? = null
}

/**
 * Marca "o usuário escolheu esta música" (tocou nela numa lista ou na fila).
 * O controller marca antes de mandar tocar; o serviço confere quando a música
 * começa. Controller e serviço rodam no mesmo processo do app.
 */
internal object ChosenSongSignal {
    @Volatile
    private var pending: String? = null

    fun mark(key: String) {
        pending = key
    }

    /** true se [key] foi a música escolhida (e esquece a marca). */
    fun consume(key: String): Boolean {
        val chosen = pending == key
        if (chosen) pending = null
        return chosen
    }
}

/**
 * Regras do "mais ouvidas" (classe pura, testada):
 * - Conta se a música foi **escolhida** pelo usuário (tocar nela numa lista ou
 *   na fila, com ou sem aleatório) ou se tocou na **ordem normal**.
 * - **Não** conta se entrou sozinha pelo **aleatório**.
 * - Só conta depois de **30 s ouvidos de verdade** (pular para a frente não
 *   soma). Música com menos de 30 s conta com 80% dela ouvida.
 * - Conta **uma vez** por reprodução.
 */
internal class PlayCountTracker(private val minListenMs: Long = 30_000) {
    private var key: String? = null
    private var eligible = false
    private var counted = false
    private var listenedReported = false
    private var listenedMs = 0L
    private var playingSinceMs: Long? = null
    private var thresholdMs = minListenMs
    private var reportedMs = 0L

    /** Uma música começou (nova ou repetida). */
    fun start(key: String, durationMs: Long, chosen: Boolean, shuffle: Boolean, isPlaying: Boolean, nowMs: Long) {
        this.key = key
        eligible = chosen || !shuffle
        counted = false
        listenedReported = false
        listenedMs = 0
        reportedMs = 0
        playingSinceMs = if (isPlaying) nowMs else null
        thresholdMs = if (durationMs in 1 until minListenMs) (durationMs * 8 / 10) else minListenMs
    }

    fun setPlaying(isPlaying: Boolean, nowMs: Long) {
        val since = playingSinceMs
        if (isPlaying && since == null) {
            playingSinceMs = nowMs
        } else if (!isPlaying && since != null) {
            listenedMs += nowMs - since
            playingSinceMs = null
        }
    }

    /**
     * Histórico: devolve a chave uma vez, quando a música passa do tempo
     * mínimo ouvido, valha ou não para "mais ouvidas".
     */
    fun checkListened(nowMs: Long): String? {
        if (listenedReported) return null
        val total = listenedMs + (playingSinceMs?.let { nowMs - it } ?: 0L)
        if (total < thresholdMs) return null
        listenedReported = true
        return key
    }

    /** Devolve a chave da música no momento em que ela passa a contar; senão null. */
    fun check(nowMs: Long): String? {
        if (!eligible || counted) return null
        val total = listenedMs + (playingSinceMs?.let { nowMs - it } ?: 0L)
        if (total < thresholdMs) return null
        counted = true
        return key
    }

    /**
     * Resumo diário: o tempo ouvido desta música desde a última vez que foi
     * pedido, ou 0 se for menos que [minMs] (para não gravar a cada segundo).
     * Vale para qualquer música, ouvida 30 s ou pulada.
     */
    fun takeListenedMs(nowMs: Long, minMs: Long): Long {
        if (key == null) return 0
        val total = listenedMs + (playingSinceMs?.let { nowMs - it } ?: 0L)
        val delta = total - reportedMs
        if (delta < minMs || delta <= 0) return 0
        reportedMs = total
        return delta
    }
}
