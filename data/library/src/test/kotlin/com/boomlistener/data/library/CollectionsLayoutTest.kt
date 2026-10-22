// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.library

import org.junit.Assert.assertEquals
import org.junit.Test

class CollectionsLayoutTest {
    @Test
    fun padraoEAlbunsEmGradeEORestoEmLista() {
        assertEquals(setOf("albums"), MemoryCollectionsLayout().gridKinds.value)
    }

    @Test
    fun trocarUmaPilulaNaoMexeNasOutras() {
        val layout = MemoryCollectionsLayout()
        layout.setGrid("artists", true)
        layout.setGrid("albums", false)
        assertEquals(setOf("artists"), layout.gridKinds.value)
        layout.setGrid("artists", true)
        assertEquals(setOf("artists"), layout.gridKinds.value)
    }
}
