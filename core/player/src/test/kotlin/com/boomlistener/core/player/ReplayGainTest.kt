// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReplayGainTest {
    @Test
    fun `reads track gain in any case and format`() {
        assertEquals(-6.48f, replayGainDb(listOf("replaygain_track_gain" to "-6.48 dB"))!!, 0.001f)
        assertEquals(-3.5f, replayGainDb(listOf("REPLAYGAIN_TRACK_GAIN" to "-3,50 dB"))!!, 0.001f)
        assertEquals(2.1f, replayGainDb(listOf("ReplayGain_Track_Gain" to "+2.10 dB"))!!, 0.001f)
    }

    @Test
    fun `ignores album gain and other tags`() {
        assertNull(replayGainDb(listOf("REPLAYGAIN_ALBUM_GAIN" to "-4 dB", "TITLE" to "x")))
    }

    @Test
    fun `opus r128 gain is converted`() {
        // -2560/256 = -10 dB em R128 → -5 dB em ReplayGain.
        assertEquals(-5f, replayGainDb(listOf("R128_TRACK_GAIN" to "-2560"))!!, 0.001f)
    }

    @Test
    fun `volume only goes down`() {
        assertEquals(1f, replayGainVolume(null), 0.0001f)
        assertEquals(0.5012f, replayGainVolume(-6f), 0.001f)
        assertEquals(1f, replayGainVolume(4f), 0.0001f)
    }
}
