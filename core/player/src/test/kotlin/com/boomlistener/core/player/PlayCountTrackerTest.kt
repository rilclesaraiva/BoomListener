// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayCountTrackerTest {
    private val minute = 180_000L

    @Test
    fun contaDepoisDe30sNaOrdemNormal() {
        val tracker = PlayCountTracker()
        tracker.start("a", minute, chosen = false, shuffle = false, isPlaying = true, nowMs = 0)
        assertNull(tracker.check(29_000))
        assertEquals("a", tracker.check(30_000))
        assertNull(tracker.check(60_000)) // uma vez só
    }

    @Test
    fun aleatorioSozinhoNaoConta() {
        val tracker = PlayCountTracker()
        tracker.start("a", minute, chosen = false, shuffle = true, isPlaying = true, nowMs = 0)
        assertNull(tracker.check(120_000))
    }

    @Test
    fun escolhidaDentroDoAleatorioConta() {
        val tracker = PlayCountTracker()
        tracker.start("a", minute, chosen = true, shuffle = true, isPlaying = true, nowMs = 0)
        assertEquals("a", tracker.check(31_000))
    }

    @Test
    fun pausaNaoSomaTempo() {
        val tracker = PlayCountTracker()
        tracker.start("a", minute, chosen = true, shuffle = false, isPlaying = true, nowMs = 0)
        tracker.setPlaying(false, 20_000) // ouviu 20 s
        assertNull(tracker.check(100_000)) // pausado: continua 20 s
        tracker.setPlaying(true, 100_000)
        assertNull(tracker.check(109_000)) // 29 s
        assertEquals("a", tracker.check(110_000)) // 30 s
    }

    @Test
    fun musicaCurtaContaCom80Porcento() {
        val tracker = PlayCountTracker()
        tracker.start("curta", 20_000, chosen = false, shuffle = false, isPlaying = true, nowMs = 0)
        assertNull(tracker.check(15_000))
        assertEquals("curta", tracker.check(16_000))
    }

    @Test
    fun repetirComecaDoZero() {
        val tracker = PlayCountTracker()
        tracker.start("a", minute, chosen = true, shuffle = false, isPlaying = true, nowMs = 0)
        assertEquals("a", tracker.check(40_000))
        tracker.start("a", minute, chosen = false, shuffle = false, isPlaying = true, nowMs = 180_000)
        assertNull(tracker.check(200_000))
        assertEquals("a", tracker.check(210_000))
    }
}
