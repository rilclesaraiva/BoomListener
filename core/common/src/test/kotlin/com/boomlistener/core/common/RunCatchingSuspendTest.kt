// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

class RunCatchingSuspendTest {
    @Test
    fun devolveOValorQuandoDaCerto() {
        assertEquals(7, runCatchingSuspend { 7 }.getOrNull())
    }

    @Test
    fun falhaComumViraResultadoDeFalha() {
        val result = runCatchingSuspend<Int> { error("sem internet") }
        assertTrue(result.isFailure)
        assertNull(result.getOrNull())
    }

    @Test
    fun cancelamentoEhRelancadoEmVezDeEngolido() {
        try {
            runCatchingSuspend<Int> { throw CancellationException("saiu da tela") }
            fail("o cancelamento deveria ser relançado")
        } catch (e: CancellationException) {
            assertEquals("saiu da tela", e.message)
        }
    }
}
