// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmoothSeekBarTest {
    @Test
    fun andaJuntoComORelogio() {
        // Bolinha no ponto certo: depois de um quadro de 16 ms, está 16 ms à frente.
        val next = nextSmoothPositionMs(shownMs = 10_016f, reportedMs = 10_000, sinceReportMs = 32f, frameMs = 16f, speed = 1f, durationMs = 200_000)
        assertEquals(10_032f, next, 0.5f)
    }

    @Test
    fun correcaoPequenaAosPoucos() {
        // O player informou um ponto 200 ms à frente: a bolinha não salta, vai chegando.
        val next = nextSmoothPositionMs(shownMs = 10_000f, reportedMs = 10_200, sinceReportMs = 0f, frameMs = 16f, speed = 1f, durationMs = 200_000)
        assertTrue(next > 10_016f && next < 10_200f)
    }

    @Test
    fun saltoDeVerdadeVaiDireto() {
        val next = nextSmoothPositionMs(shownMs = 10_000f, reportedMs = 90_000, sinceReportMs = 0f, frameMs = 16f, speed = 1f, durationMs = 200_000)
        assertEquals(90_000f, next, 0.001f)
    }

    @Test
    fun semAvisoNovoNaoPassaDoPonto() {
        // Travou (carregando): depois de 1 s sem aviso, a bolinha espera.
        var shown = 10_000f
        repeat(300) { frame ->
            shown = nextSmoothPositionMs(shown, reportedMs = 10_000, sinceReportMs = frame * 16f, frameMs = 16f, speed = 1f, durationMs = 200_000)
        }
        assertTrue(shown <= 11_200f)
    }

    @Test
    fun velocidadeDobrada() {
        val next = nextSmoothPositionMs(shownMs = 10_032f, reportedMs = 10_000, sinceReportMs = 16f, frameMs = 16f, speed = 2f, durationMs = 200_000)
        assertEquals(10_064f, next, 5f)
    }

    @Test
    fun naoPassaDoFim() {
        val next = nextSmoothPositionMs(shownMs = 199_990f, reportedMs = 199_990, sinceReportMs = 100f, frameMs = 16f, speed = 1f, durationMs = 200_000)
        assertTrue(next <= 200_000f)
    }
}
