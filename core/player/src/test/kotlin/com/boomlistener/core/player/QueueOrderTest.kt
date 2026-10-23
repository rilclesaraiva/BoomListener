// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.player

import org.junit.Assert.assertEquals
import org.junit.Test

class QueueOrderTest {
    private val songs = listOf("a", "b", "c", "d", "e")

    @Test
    fun escolhidaVemPrimeiroDepoisAsSeguintesEDepoisAsAnteriores() {
        assertEquals(listOf("c", "d", "e", "a", "b"), queueStartingAt(songs, 2))
    }

    @Test
    fun ultimaDaListaVaiParaOTopoComAsOutrasEmbaixo() {
        assertEquals(listOf("e", "a", "b", "c", "d"), queueStartingAt(songs, 4))
    }

    @Test
    fun primeiraOuIndiceInvalidoMantemAOrdem() {
        assertEquals(songs, queueStartingAt(songs, 0))
        assertEquals(songs, queueStartingAt(songs, 9))
        assertEquals(emptyList<String>(), queueStartingAt(emptyList<String>(), 0))
    }
}
