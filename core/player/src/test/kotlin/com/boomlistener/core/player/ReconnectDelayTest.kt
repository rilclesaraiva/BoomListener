// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReconnectDelayTest {
    @Test
    fun esperaCresceAteCincoSegundosEDesiste() {
        assertEquals(1_000L, reconnectDelayMs(0))
        assertEquals(2_000L, reconnectDelayMs(1))
        assertEquals(5_000L, reconnectDelayMs(4))
        assertEquals(5_000L, reconnectDelayMs(5))
        assertNull(reconnectDelayMs(6))
    }
}
