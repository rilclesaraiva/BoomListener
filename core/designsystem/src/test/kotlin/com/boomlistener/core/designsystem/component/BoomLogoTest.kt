// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoomLogoTest {
    @Test
    fun noFimOIconeEstaCompletoEParado() {
        val f = boomLogoFrame(BoomLogoMs)
        assertEquals(0f, f.jetX, 0.001f)
        assertEquals(1f, f.dotScaleX, 0.001f)
        assertEquals(1f, f.dotScaleY, 0.001f)
        assertEquals(0f, f.shakeX, 0.001f)
        assertEquals(0f, f.shakeY, 0.001f)
        assertEquals(1f, f.arc1Scale, 0.001f)
        assertEquals(1f, f.arc2Scale, 0.001f)
        assertEquals(1f, f.arc1Alpha, 0.001f)
        assertEquals(1f, f.arc2Alpha, 0.001f)
        assertEquals(0f, f.flashAlpha, 0.001f)
        assertEquals(0f, f.coneAlpha, 0.001f)
        assertEquals(0f, f.flyAlpha, 0.001f)
        f.rings.forEach { assertEquals(0f, it.alpha, 0.001f) }
    }

    @Test
    fun noComecoOCacaEstaForaEOsArcosAindaNao() {
        val f = boomLogoFrame(0f)
        assertEquals(60f, f.jetX, 0.001f)
        assertEquals(0f, f.arc1Alpha, 0.001f)
        assertEquals(0f, f.arc2Alpha, 0.001f)
    }

    @Test
    fun oEstrondoVemDepoisDeOCacaChegar() {
        // Em 400 ms o caça já parou no lugar, mas os arcos ainda não saltaram.
        val parado = boomLogoFrame(400f)
        assertEquals(0f, parado.jetX, 0.5f)
        assertEquals(0f, parado.arc1Alpha, 0.001f)
        // Depois do estrondo (1000 ms) eles já estão à vista e o rastro sumiu.
        val estrondo = boomLogoFrame(1000f)
        assertTrue(estrondo.arc1Alpha > 0.99f)
        assertEquals(0f, estrondo.flyAlpha, 0.001f)
    }

    @Test
    fun aAnimacaoTerminaQuandoOUltimoEcoAcaba() {
        // O último eco começa em 980 ms e dura 1000 ms: acaba em 1980, antes dos 2000.
        assertTrue(boomLogoFrame(1990f).rings[2].alpha < 0.001f)
        assertTrue(boomLogoFrame(1500f).rings[2].alpha > 0f)
    }
}
