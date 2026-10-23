// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.player

/**
 * A fila ao tocar uma música escolhida numa lista: ela vem primeiro, depois as
 * que vinham depois dela e, por fim, as que vinham antes. Assim a escolhida é
 * o topo da fila (tocar a última da lista não deixa a fila "no fim") e
 * nenhuma música da lista some.
 */
internal fun <T> queueStartingAt(items: List<T>, index: Int): List<T> {
    if (items.isEmpty() || index !in items.indices || index == 0) return items
    return items.subList(index, items.size) + items.subList(0, index)
}
