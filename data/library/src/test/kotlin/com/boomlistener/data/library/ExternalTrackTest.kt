// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExternalTrackTest {
    @Test
    fun tituloSemExtensao() {
        assertEquals("A way(M4A_128K)", titleFromFileName("A way(M4A_128K).m4a"))
        assertEquals("12AM (feat. Day_okee _ REDEMBRECE)", titleFromFileName("12AM (feat. Day_okee _ REDEMBRECE).mp3"))
        assertEquals("Canção.sem extensão longa", titleFromFileName("Canção.sem extensão longa"))
        assertEquals("faixa", titleFromFileName("faixa"))
        assertNull(titleFromFileName("  "))
        assertNull(titleFromFileName(null))
    }

    @Test
    fun numeroDaFaixaComTotal() {
        assertEquals(3, leadingNumber("3/12"))
        assertEquals(7, leadingNumber(" 7 "))
        assertNull(leadingNumber("0"))
        assertNull(leadingNumber("abc"))
        assertNull(leadingNumber(null))
    }
}
