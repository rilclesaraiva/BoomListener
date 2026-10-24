// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareWaveTest {
    @Test
    fun `waveform follows the loudness of the clip`() {
        // Metade baixa, metade alta.
        val levels = FloatArray(600) { if (it < 300) 0.1f else 0.5f }
        val bars = waveformBars(levels, bars = 10)
        assertEquals(10, bars.size)
        assertTrue(bars[0] < bars[9])
        assertEquals(1f, bars[9], 0.0001f)
    }

    @Test
    fun `silence gives the minimum height`() {
        val bars = waveformBars(FloatArray(600), bars = 5)
        bars.forEach { assertEquals(MIN_BAR, it, 0.0001f) }
    }

    @Test
    fun `no levels gives flat bars`() {
        assertEquals(WAVE_BARS, waveformBars(FloatArray(0)).size)
        pulseBars(FloatArray(0), 1000).forEach { assertEquals(MIN_BAR, it, 0.0001f) }
    }

    @Test
    fun `pulse center shows the current instant and is symmetric`() {
        val levels = FloatArray(600) { 0.1f }.also { it[100] = 1f } // pico em 5 s
        val bars = pulseBars(levels, timeMs = 5_000, bars = 9)
        assertEquals(1f, bars[4], 0.0001f)
        for (i in 0 until 4) assertEquals(bars[i], bars[8 - i], 0.0001f)
        assertTrue(bars[0] < bars[4])
    }

    @Test
    fun `pulse outside the clip is silent`() {
        val levels = FloatArray(10) { 1f }
        assertEquals(MIN_BAR, pulseBars(levels, timeMs = 60_000, bars = 3)[1], 0.0001f)
    }
}
