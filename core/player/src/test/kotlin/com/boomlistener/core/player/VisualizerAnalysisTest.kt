// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.player

import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualizerAnalysisTest {
    private val rate = 44_100

    private fun sine(hz: Double, amplitude: Float = 1f) =
        FloatArray(VISUALIZER_WINDOW) { i -> (amplitude * sin(2 * PI * hz * i / rate)).toFloat() }

    /** A barra em que cai [hz] (mesma escala logarítmica do visualizador). */
    private fun bandOf(hz: Double): Int {
        val top = 16_000.0
        return (0 until VISUALIZER_BANDS).first { band ->
            val high = 40.0 * (top / 40.0).pow((band + 1).toDouble() / VISUALIZER_BANDS)
            hz < high
        }
    }

    @Test
    fun graveAEsquerdaAgudoADireita() {
        val bass = visualizerBands(sine(80.0), rate)
        val treble = visualizerBands(sine(8_000.0), rate)
        val bassPeak = bass.indices.maxBy { bass[it] }
        val treblePeak = treble.indices.maxBy { treble[it] }
        // Perto da faixa certa (os graves têm pouca resolução numa janela de 1024).
        assertTrue(kotlin.math.abs(bassPeak - bandOf(80.0)) <= 1)
        assertTrue(kotlin.math.abs(treblePeak - bandOf(8_000.0)) <= 1)
        assertTrue(bassPeak < treblePeak)
    }

    @Test
    fun volumeMaximoChegaPertoDoTopo() {
        val bands = visualizerBands(sine(1_000.0), rate)
        assertTrue(bands.max() > 0.9f)
    }

    @Test
    fun maisBaixoBarraMenor() {
        val loud = visualizerBands(sine(1_000.0, 1f), rate).max()
        val quiet = visualizerBands(sine(1_000.0, 0.05f), rate).max()
        assertTrue(quiet < loud)
    }

    @Test
    fun silencioZeraTudo() {
        val bands = visualizerBands(FloatArray(VISUALIZER_WINDOW), rate)
        assertTrue(bands.all { it == 0f })
        assertTrue(waveformPoints(FloatArray(VISUALIZER_WINDOW)).all { it == 0f })
    }

    @Test
    fun ondaTemOsPontosDoSom() {
        val wave = waveformPoints(sine(440.0))
        assertEquals(VISUALIZER_WAVE_POINTS, wave.size)
        assertTrue(wave.all { it in -1f..1f })
        assertTrue(wave.any { it > 0.3f } && wave.any { it < -0.3f })
    }

    @Test
    fun sobeRapidoDesceDevagar() {
        val up = smoothVisualizer(0f, 1f)
        val down = smoothVisualizer(1f, 0f)
        assertTrue(up > 1f - down) // subiu mais do que desceu
    }
}
