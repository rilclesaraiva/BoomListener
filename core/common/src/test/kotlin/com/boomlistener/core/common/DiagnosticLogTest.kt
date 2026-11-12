// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class DiagnosticLogTest {
    @Test
    fun linhaTemHoraTipoEMensagem() {
        assertEquals("12:00:01.234 teclado: visível\n", formatLogLine("12:00:01.234", "teclado", "visível"))
    }

    @Test
    fun textoCurtoFicaIgual() {
        assertEquals("a\nb\n", trimLog("a\nb\n", 100))
    }

    @Test
    fun cortaPeloComecoEComecaNumaLinhaInteira() {
        val text = "linha1\nlinha2\nlinha3\n"
        // Cabem 12 caracteres do fim ("ha2\nlinha3\n" cortaria no meio de "linha2").
        assertEquals("linha3\n", trimLog(text, 12))
    }

    @Test
    fun semQuebraDeLinhaNoPedacoDevolveOPedaco() {
        assertEquals("cdef", trimLog("abcdef", 4))
    }
}
