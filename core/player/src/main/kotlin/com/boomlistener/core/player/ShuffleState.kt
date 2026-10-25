// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.player

import kotlin.random.Random

/**
 * O embaralhamento próprio do app (ver docs/ARQUITETURA.md). Classe pura, sem
 * Android, para ser testada à parte.
 *
 * - Embaralha com Fisher-Yates: cada item aparece **uma vez** por rodada.
 * - A ordem embaralhada vira a fila de verdade (o player não embaralha nada).
 * - Guarda a ordem original para restaurar quando o aleatório é desligado.
 *
 * Os itens são identificados por [keyOf] (o endereço da música), porque as
 * cópias que voltam do player não são o mesmo objeto.
 */
internal class ShuffleState<T>(
    private val keyOf: (T) -> String,
    private val random: Random = Random.Default,
) {
    /** Ordem original da fila enquanto o aleatório está ligado; null se desligado. */
    private var original: MutableList<T>? = null

    val isActive: Boolean
        get() = original != null

    /**
     * Liga o aleatório. [items] é a fila na ordem atual e [currentIndex] a
     * música que está tocando (ou a escolhida para começar). Devolve a nova
     * ordem: essa música primeiro, o resto embaralhado.
     */
    fun start(items: List<T>, currentIndex: Int): List<T> {
        original = items.toMutableList()
        if (items.isEmpty()) return items
        val first = currentIndex.coerceIn(items.indices)
        val rest = items.filterIndexed { index, _ -> index != first }
        return listOf(items[first]) + fisherYates(rest)
    }

    /** Uma música saiu da fila: sai também da ordem original. */
    fun remove(item: T) {
        val list = original ?: return
        val key = keyOf(item)
        val index = list.indexOfFirst { keyOf(it) == key }
        if (index >= 0) list.removeAt(index)
    }

    /**
     * Desliga o aleatório. Devolve as músicas que vêm **antes** e **depois** de
     * [current] na ordem original, para remontar a fila em volta da música que
     * está tocando sem interrompê-la. null se não havia ordem guardada ou se a
     * música atual não está nela.
     */
    fun stop(current: T?): Pair<List<T>, List<T>>? {
        val list = original ?: return null
        original = null
        if (current == null) return null
        val key = keyOf(current)
        val position = list.indexOfFirst { keyOf(it) == key }
        if (position < 0) return null
        return list.subList(0, position).toList() to list.subList(position + 1, list.size).toList()
    }

    /**
     * Uma música entrou na fila logo depois de [anchor] ("adicionar à fila"):
     * entra no mesmo ponto da ordem original. Sem [anchor] (ou se ela não está
     * na ordem), vai para o fim.
     */
    fun insertAfter(anchor: T?, item: T) {
        val list = original ?: return
        val position = anchor?.let { a -> list.indexOfFirst { keyOf(it) == keyOf(a) } } ?: -1
        if (position >= 0) list.add(position + 1, item) else list.add(item)
    }

    /** A ordem original guardada (null com o aleatório desligado). */
    fun originalItems(): List<T>? = original?.toList()

    /** Volta a um aleatório já em andamento (app reaberto): [order] é a ordem original. */
    fun adopt(order: List<T>) {
        original = order.toMutableList()
    }

    /** Esquece tudo (fila trocada ou encerrada). */
    fun clear() {
        original = null
    }

    /**
     * Nova rodada no "repetir tudo": mantém a primeira música (a que acabou de
     * começar) e embaralha o resto de novo.
     */
    fun newRound(order: List<T>): List<T> =
        if (order.size <= 2) order else listOf(order.first()) + fisherYates(order.drop(1))

    private fun fisherYates(items: List<T>): List<T> {
        val result = items.toMutableList()
        for (i in result.lastIndex downTo 1) {
            val j = random.nextInt(i + 1)
            val tmp = result[i]
            result[i] = result[j]
            result[j] = tmp
        }
        return result
    }
}

/**
 * O aleatório do app é um só no processo: o controller (telas) mexe nele e o
 * serviço guarda/restaura a ordem original junto com a fila ("lembrar o
 * aleatório ao reabrir").
 */
internal object SharedShuffle {
    val state = ShuffleState<androidx.media3.common.MediaItem>(keyOf = { it.mediaId })
}
