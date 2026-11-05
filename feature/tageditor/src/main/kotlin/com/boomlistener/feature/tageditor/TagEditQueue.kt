// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.tageditor

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Fila de músicas a editar em sequência (a seleção múltipla das listas). */
internal data class TagQueue(val uris: List<String>, val index: Int = 0) {
    val current: String? get() = uris.getOrNull(index)
    val size: Int get() = uris.size
    val position: Int get() = index + 1
    val next: TagQueue? get() = if (index + 1 < uris.size) copy(index = index + 1) else null
}

/**
 * A fila em andamento. Fica só na memória: se o app for fechado, o editor
 * volta a abrir uma música só (nada do que está guardado muda).
 */
object TagEditQueue {
    private val _state = MutableStateFlow<TagQueue?>(null)
    internal val state: StateFlow<TagQueue?> = _state

    /** Começa a fila; com uma música só, não há fila. */
    fun start(uris: List<String>) {
        val distinct = uris.distinct()
        _state.value = if (distinct.size > 1) TagQueue(distinct) else null
    }

    /** Passa para a próxima; devolve o endereço dela, ou null (e encerra) se acabou. */
    internal fun advance(): String? {
        val next = _state.value?.next
        _state.value = next
        return next?.current
    }

    fun clear() {
        _state.value = null
    }
}
