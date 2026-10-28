// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StereoHistoryTest {
    private fun filled(frames: Int, rate: Int = 1000, startUs: Long = 0L, capacity: Int = 1 shl 12) =
        StereoHistory(capacity).apply {
            mark(startUs, rate)
            repeat(frames) { push(it.toFloat(), -it.toFloat()) }
        }

    @Test
    fun achaAAmostraQueTocaAgora() {
        val history = filled(1000) // 1 s a 1000 amostras/s
        assertEquals(0L, history.frameAt(0))
        assertEquals(250L, history.frameAt(250_000))
        assertEquals(999L, history.frameAt(999_000))
    }

    @Test
    fun antesDaPrimeiraMarcaNaoHaNada() {
        val history = filled(100, startUs = 5_000_000)
        assertEquals(-1L, history.frameAt(4_000_000))
    }

    @Test
    fun nuncaPassaDoQueJaEntrou() {
        val history = filled(100)
        assertEquals(100L, history.frameAt(10_000_000))
    }

    @Test
    fun usaAMarcaMaisRecenteAteAqueleMomento() {
        val history = StereoHistory(1 shl 12)
        history.mark(0, 1000)
        repeat(100) { history.push(0f, 0f) }
        // Pulo na música: o pedaço seguinte é do segundo 60.
        history.mark(60_000_000, 1000)
        repeat(100) { history.push(1f, 1f) }
        assertEquals(50L, history.frameAt(50_000))
        assertEquals(110L, history.frameAt(60_010_000))
    }

    @Test
    fun marcasApagadasNaoValemMais() {
        val history = filled(100)
        history.clearMarks()
        assertEquals(-1L, history.frameAt(50_000))
    }

    @Test
    fun copiaEsquerdoEDireito() {
        val history = filled(100)
        val l = FloatArray(10)
        val r = FloatArray(10)
        assertEquals(5, history.copy(20, 25, l, r))
        assertEquals(20f, l[0])
        assertEquals(-24f, r[4])
    }

    @Test
    fun copiaSoAsMaisRecentesQuandoNaoCabe() {
        val history = filled(100)
        val l = FloatArray(10)
        val r = FloatArray(10)
        assertEquals(10, history.copy(0, 100, l, r))
        assertEquals(90f, l[0])
        assertEquals(99f, l[9])
    }

    @Test
    fun oQueSaiuDoHistoricoNaoVolta() {
        val history = filled(100, capacity = 64)
        val l = FloatArray(200)
        val r = FloatArray(200)
        assertEquals(64, history.copy(0, 100, l, r))
        assertEquals(36f, l[0])
    }

    @Test
    fun nadaParaCopiarQuandoOIntervaloEstaVazio() {
        val history = filled(100)
        assertEquals(0, history.copy(50, 50, FloatArray(4), FloatArray(4)))
        assertEquals(0, history.copy(200, 300, FloatArray(4), FloatArray(4)))
    }

    @Test
    fun saltoLongoFicaFracoECurtoAcende() {
        assertEquals(1f, beamIntensity(1f, 2f))
        assertEquals(1f, beamIntensity(2f, 2f))
        assertEquals(0.1f, beamIntensity(20f, 2f), 1e-6f)
        assertTrue(beamIntensity(200f, 2f) < 0.02f)
    }

    @Test
    fun niveisDeBrilho() {
        assertEquals(0, beamLevel(1f))
        assertEquals(0, beamLevel(0.8f))
        assertEquals(1, beamLevel(0.5f))
        assertEquals(2, beamLevel(0.25f))
        assertEquals(3, beamLevel(0.12f))
        assertEquals(-1, beamLevel(0.05f))
    }
}
