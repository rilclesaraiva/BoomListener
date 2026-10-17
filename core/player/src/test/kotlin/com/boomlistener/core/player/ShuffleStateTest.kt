// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ShuffleStateTest {
    private val songs = (1..50).map { "musica-$it" }
    private fun newState(seed: Int = 7) = ShuffleState<String>(keyOf = { it }, random = Random(seed))

    @Test
    fun cadaMusicaApareceUmaVezENenhumaSeRepete() {
        repeat(20) { seed ->
            val order = newState(seed).start(songs, currentIndex = 10)
            assertEquals(songs.size, order.size)
            assertEquals(songs.toSet(), order.toSet())
            assertEquals(order.size, order.distinct().size)
        }
    }

    @Test
    fun aMusicaQueEstaTocandoVemPrimeiro() {
        val order = newState().start(songs, currentIndex = 10)
        assertEquals("musica-11", order.first())
    }

    @Test
    fun embaralhaDeVerdade() {
        val order = newState().start(songs, currentIndex = 0)
        assertTrue(order != songs)
    }

    @Test
    fun desligarDevolveAOrdemOriginalEmVoltaDaMusicaAtual() {
        val state = newState()
        state.start(songs, currentIndex = 0)
        val (before, after) = state.stop(current = "musica-20")!!
        assertEquals(songs.subList(0, 19), before)
        assertEquals(songs.subList(20, 50), after)
        assertFalse(state.isActive)
    }

    @Test
    fun musicaRemovidaNoAleatorioSaiTambemDaOrdemOriginal() {
        val state = newState()
        state.start(songs, currentIndex = 0)
        state.remove("musica-5")
        state.remove("musica-30")
        val (before, after) = state.stop(current = "musica-1")!!
        val restored = before + "musica-1" + after
        assertEquals(songs - "musica-5" - "musica-30", restored)
    }

    @Test
    fun semOrdemGuardadaNaoHaoQueRestaurar() {
        assertNull(newState().stop(current = "musica-1"))
    }

    @Test
    fun novaRodadaMantemAPrimeiraESemRepeticao() {
        val state = newState()
        val first = state.start(songs, currentIndex = 3)
        val round = state.newRound(first)
        assertEquals(first.first(), round.first())
        assertEquals(songs.toSet(), round.toSet())
        assertEquals(round.size, round.distinct().size)
    }
}
