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
    fun quadroEntreDuasMedidasMistura() {
        val frames = arrayOf(FloatArray(SOUND_WAVE_POINTS) { 0f }, FloatArray(SOUND_WAVE_POINTS) { 1f })
        assertEquals(0.5f, soundWaveAt(frames, LEVEL_WINDOW_MS / 2)[10], 0.001f)
        // Depois da última medida, fica nela.
        assertEquals(1f, soundWaveAt(frames, 10_000)[10], 0.001f)
    }

    @Test
    fun medidaMaisForte() {
        assertEquals(2, loudestFrame(floatArrayOf(0.1f, 0.3f, 0.9f, 0.2f)))
        assertEquals(0, loudestFrame(FloatArray(0)))
    }
}
