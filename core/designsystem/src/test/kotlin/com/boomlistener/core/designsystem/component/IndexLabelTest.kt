// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Test

class IndexLabelTest {
    @Test
    fun letraSemAcento() {
        assertEquals("A", boomIndexLabel("Ávila"))
        assertEquals("E", boomIndexLabel("éramos"))
        assertEquals("C", boomIndexLabel("Ção"))
    }

    @Test
    fun ignoraSimbolosDoComeco() {
        assertEquals("H", boomIndexLabel("\"Heroes\""))
        assertEquals("B", boomIndexLabel("  (bonus) track"))
    }

    @Test
    fun numerosEOutrosAlfabetosViramCerquilha() {
        assertEquals("#", boomIndexLabel("99 Luftballons"))
        assertEquals("#", boomIndexLabel("夜に駆ける"))
        assertEquals("#", boomIndexLabel(""))
        assertEquals("#", boomIndexLabel(null))
    }
}
