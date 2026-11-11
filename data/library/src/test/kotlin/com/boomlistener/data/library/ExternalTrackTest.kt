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

    @Test
    fun nomeDaCopiaSemCaracteresPerigosos() {
        val name = safeCopyName("../a/b:c*d?.m4a", "content://x/1")
        assertEquals(false, name.contains('/'))
        assertEquals(false, name.contains(':'))
        assertEquals(true, name.endsWith(".m4a"))
    }

    @Test
    fun nomeDaCopiaMantemAcentosELimitaOTamanho() {
        assertEquals(true, safeCopyName("Canção.mp3", "content://x/1").endsWith("_Canção.mp3"))
        val long = safeCopyName("a".repeat(300) + ".mp3", "content://x/1")
        assertEquals(true, long.length < 100)
        assertEquals(true, long.endsWith(".mp3"))
    }

    @Test
    fun enderecosDiferentesNaoSeMisturam() {
        assertEquals(false, safeCopyName("a.mp3", "content://x/1") == safeCopyName("a.mp3", "content://y/2"))
    }
}
