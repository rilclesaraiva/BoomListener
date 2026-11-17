// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniPlayerMagnetTest {
    @Test
    fun semPuxarFicaNoLugar() {
        assertEquals(0f, magnetOffset(0f, 100f), 0.001f)
        assertEquals(0f, magnetOffset(-5f, 100f), 0.001f)
    }

    @Test
    fun dentroDoImaCedeSoUmaFracao() {
        assertEquals(10f, magnetOffset(40f, 100f), 0.001f)
        assertTrue(magnetOffset(100f, 100f) < 100f * 0.5f)
    }

    @Test
    fun passandoDoImaSegueODedoSemSalto() {
        val atEdge = magnetOffset(100f, 100f)
        assertEquals(25f, atEdge, 0.001f)
        assertEquals(atEdge + 30f, magnetOffset(130f, 100f), 0.001f)
    }

    @Test
    fun crescenteSempre() {
        var last = -1f
        for (pull in 0..300 step 10) {
            val v = magnetOffset(pull.toFloat(), 100f)
            assertTrue(v >= last)
            last = v
        }
    }
}
