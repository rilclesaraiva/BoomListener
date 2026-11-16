// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Test

class BoomImageCycleTest {
    @Test
    fun `volta ao começo depois da última imagem`() {
        assertEquals(1, nextCycleIndex(0, 3))
        assertEquals(2, nextCycleIndex(1, 3))
        assertEquals(0, nextCycleIndex(2, 3))
    }

    @Test
    fun `lista de uma imagem ou vazia fica no zero`() {
        assertEquals(0, nextCycleIndex(0, 1))
        assertEquals(0, nextCycleIndex(0, 0))
    }
}
