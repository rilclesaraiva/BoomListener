// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.library

import org.junit.Assert.assertEquals
import org.junit.Test

class FavoritesTest {
    @Test
    fun favoritadaPorUltimoVemPrimeiroEDesfavoritarTira() {
        var list = toggleFavorite(emptyList(), "content://1")
        list = toggleFavorite(list, "content://2")
        assertEquals(listOf("content://2", "content://1"), list)
        list = toggleFavorite(list, "content://1")
        assertEquals(listOf("content://2"), list)
    }
}
