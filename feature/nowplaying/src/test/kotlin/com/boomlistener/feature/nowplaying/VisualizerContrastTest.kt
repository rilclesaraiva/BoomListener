// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualizerContrastTest {
    private val peach = 0xFFFFB98A.toInt()

    @Test
    fun capaEscuraEscurecidoNormal() {
        val dark = IntArray(40) { 0xFF101010.toInt() }
        assertEquals(VISUALIZER_SCRIM_MIN, visualizerScrimAlpha(dark, peach), 0.001f)
    }

    @Test
    fun capaDaMesmaCorEscureceMais() {
        val light = IntArray(40) { 0xFFF5D9C0.toInt() }
        assertEquals(VISUALIZER_SCRIM_MAX, visualizerScrimAlpha(light, peach), 0.001f)
    }

    @Test
    fun parteParecidaEscureceUmPouco() {
        val mixed = IntArray(40) { if (it < 8) 0xFFF5D9C0.toInt() else 0xFF101010.toInt() }
        val alpha = visualizerScrimAlpha(mixed, peach)
        assertTrue(alpha > VISUALIZER_SCRIM_MIN && alpha < VISUALIZER_SCRIM_MAX)
    }

    @Test
    fun semAmostrasEscurecidoNormal() {
        assertEquals(VISUALIZER_SCRIM_MIN, visualizerScrimAlpha(IntArray(0), peach), 0.001f)
    }
}
