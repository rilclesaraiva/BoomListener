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
    }
}
