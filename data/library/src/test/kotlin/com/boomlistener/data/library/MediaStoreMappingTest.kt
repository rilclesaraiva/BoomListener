// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaStoreMappingTest {
    @Test
    fun numeroDaFaixaSemODisco() {
        assertEquals(5, parseTrackNumber(1005))
        assertEquals(12, parseTrackNumber(2012))
        assertEquals(7, parseTrackNumber(7))
    }

    @Test
    fun numeroDaFaixaAusente() {
        assertNull(parseTrackNumber(null))
        assertNull(parseTrackNumber(0))
        assertNull(parseTrackNumber(1000))
    }

    @Test
    fun tagDesconhecidaViraNull() {
        assertNull(cleanUnknownTag("<unknown>"))
        assertNull(cleanUnknownTag("   "))
        assertEquals("Djavan", cleanUnknownTag("Djavan"))
    }

    @Test
    fun nomeDoFormato() {
        assertEquals("AAC", codecName("audio/mp4a-latm"))
        assertEquals("MP3", codecName("audio/mpeg"))
        assertEquals("FLAC", codecName("audio/flac"))
        assertEquals("Opus", codecName("audio/opus"))
        assertEquals("X-MS-WMA", codecName("audio/x-ms-wma"))
        assertNull(codecName(null))
    }

    @Test
    fun taxaDeBitsEstimada() {
        // 3 MB em 4:10 dá ~96 kb/s.
        assertEquals(96, estimateBitrateKbps(3_000_000, 250_000))
        assertNull(estimateBitrateKbps(null, 250_000))
        assertNull(estimateBitrateKbps(3_000_000, 0))
    }
}
