// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackSettingsTest {
    @Test
    fun semTransicaoVolumeNormal() {
        assertEquals(1f, transitionVolume(0, 200_000, 0), 0f)
        assertEquals(1f, transitionVolume(100_000, 0, 5), 0f)
    }

    @Test
    fun sobeNoComecoEDesceNoFim() {
        // 5 s de transição numa música de 200 s.
        assertEquals(0f, transitionVolume(0, 200_000, 5), 0.001f)
        assertEquals(0.5f, transitionVolume(2_500, 200_000, 5), 0.001f)
        assertEquals(1f, transitionVolume(100_000, 200_000, 5), 0.001f)
        assertEquals(0.5f, transitionVolume(197_500, 200_000, 5), 0.001f)
        assertEquals(0f, transitionVolume(200_000, 200_000, 5), 0.001f)
    }

    @Test
    fun musicaCurtaNaoFicaSoNaTransicao() {
        // 12 s de transição numa música de 9 s: no máximo um terço (3 s).
        assertEquals(1f, transitionVolume(4_500, 9_000, 12), 0.001f)
        assertEquals(0.5f, transitionVolume(1_500, 9_000, 12), 0.001f)
    }

    @Test
    fun velocidadeDe005Em005EDentroDosLimites() {
        assertEquals(0.95f, roundSpeed(0.97f), 0.0001f)
        assertEquals(1.25f, roundSpeed(1.24f), 0.0001f)
        assertEquals(0.5f, roundSpeed(0.1f), 0.0001f)
        assertEquals(2f, roundSpeed(3f), 0.0001f)
    }
}
