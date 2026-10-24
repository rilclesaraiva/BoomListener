// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareSpectrumTest {
    private val rate = 44_100

    private fun tone(hz: Double, seconds: Double = 1.0) =
        FloatArray((rate * seconds).toInt()) { (0.5 * sin(2 * PI * hz * it / rate)).toFloat() }

    private fun loudestBar(frames: Array<FloatArray>): Int {
        // Só a escala geral: a mais alta numa medida do meio.
        val middle = frames[frames.size / 2]
        return middle.indices.maxBy { middle[it] }
    }

    @Test
    fun `bass tone lands on the left`() {
        val bar = loudestBar(spectrumFrames(tone(80.0), rate))
        assertTrue("bar $bar", bar < WAVE_BARS / 4)
    }

    @Test
    fun `treble tone lands on the right`() {
        val bar = loudestBar(spectrumFrames(tone(8_000.0), rate))
        assertTrue("bar $bar", bar > WAVE_BARS * 3 / 4)
    }

    @Test
    fun `one measure every 50 ms`() {
        assertEquals(20, spectrumFrames(tone(440.0), rate).size)
    }

    @Test
    fun `bars stay within range and interpolate`() {
        val frames = arrayOf(FloatArray(3) { 0f }, FloatArray(3) { 1f })
        val half = spectrumBars(frames, timeMs = LEVEL_WINDOW_MS / 2, bars = 3)
        half.forEach { assertEquals(MIN_BAR + (1 - MIN_BAR) * 0.5f, it, 0.001f) }
        spectrumBars(frames, timeMs = 999_999, bars = 3).forEach { assertEquals(1f, it, 0.001f) }
    }

    @Test
    fun `no frames gives flat bars`() {
        spectrumBars(emptyArray(), 0).forEach { assertEquals(MIN_BAR, it, 0.001f) }
    }

    @Test
    fun `fft finds a pure bin`() {
        val n = 64
        val re = DoubleArray(n) { kotlin.math.cos(2 * PI * 4 * it / n) }
        val im = DoubleArray(n)
        fft(re, im)
        val mags = DoubleArray(n) { kotlin.math.sqrt(re[it] * re[it] + im[it] * im[it]) }
        assertEquals(4, (0 until n / 2).maxBy { mags[it] })
    }
}
