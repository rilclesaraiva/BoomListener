// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.updates

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionTest {
    @Test
    fun comparaComoNumeroNaoComoTexto() {
        assertTrue(isNewerVersion("0.10.0", "0.9.0"))
        assertTrue(isNewerVersion("1.0.0", "0.99.99"))
        assertTrue(isNewerVersion("v0.5.1", "0.5.0"))
    }

    @Test
    fun mesmaVersaoOuMaisAntigaNaoEAtualizacao() {
        assertFalse(isNewerVersion("0.5.0", "0.5.0"))
        assertFalse(isNewerVersion("0.4.9", "0.5.0"))
        assertFalse(isNewerVersion("0.5", "0.5.0"))
    }
}
