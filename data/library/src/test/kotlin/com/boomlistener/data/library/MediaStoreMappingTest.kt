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
}
