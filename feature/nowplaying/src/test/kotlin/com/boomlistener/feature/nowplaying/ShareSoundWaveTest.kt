// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareSoundWaveTest {
    private val rate = 44_100

    @Test
    fun umaMedidaACada50msComOsPontosDoPlayer() {
        val oneSecond = FloatArray(rate) { (0.5 * sin(2 * PI * 440 * it / rate)).toFloat() }
        val frames = soundWaveFrames(oneSecond, rate)
        assertEquals(20, frames.size)
        assertTrue(frames.all { it.size == SOUND_WAVE_POINTS })
    }

    @Test
    fun trechoBaixinhoEAmpliadoAtePertoDeUm() {
        val quiet = FloatArray(rate) { (0.1 * sin(2 * PI * 220 * it / rate)).toFloat() }
        val peak = soundWaveFrames(quiet, rate).maxOf { frame -> frame.maxOf { abs(it) } }
        assertTrue("pico $peak", peak > 0.8f && peak <= 1f)
    }

    @Test
    fun silencioNaoExplode() {
        val frames = soundWaveFrames(FloatArray(rate), rate)
        assertTrue(frames.all { frame -> frame.all { it == 0f } })
        assertEquals(0, soundWaveFrames(FloatArray(0), rate).size)
    }

    @Test
    fun ondaNoInstanteExatoDoQuadro() {
        // Meio segundo de silêncio e depois meio segundo no máximo.
        val mono = FloatArray(rate) { if (it < rate / 2) 0f else 0.5f }
        val gain = soundWaveGain(mono)
        assertEquals(2f, gain, 0.001f)
        val out = FloatArray(SOUND_WAVE_POINTS)
        soundWaveAtTime(mono, rate, gain, 100, out)
        assertTrue(out.all { it == 0f })
        soundWaveAtTime(mono, rate, gain, 600, out)
        assertTrue(out.all { it == 1f })
        // Depois do fim do trecho: silêncio, sem estourar.
        soundWaveAtTime(mono, rate, gain, 5_000, out)
        assertTrue(out.all { it == 0f })
    }

    @Test
    fun medidaMaisForte() {
        assertEquals(2, loudestFrame(floatArrayOf(0.1f, 0.3f, 0.9f, 0.2f)))
        assertEquals(0, loudestFrame(FloatArray(0)))
    }
}
