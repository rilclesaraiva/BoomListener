// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BoomSelectionTest {
    @Test
    fun tocarMarcaETocarDeNovoDesmarca() {
        val s = BoomSelection()
        assertFalse(s.isActive)
        s.toggle("a")
        assertTrue(s.isActive && s.isSelected("a"))
        s.toggle("a")
        assertFalse(s.isActive)
    }

    @Test
    fun selecionarTudoELimpar() {
        val s = BoomSelection()
        s.selectAll(listOf("a", "b", "c"))
        assertEquals(3, s.count)
        s.clear()
        assertEquals(0, s.count)
    }

    @Test
    fun retainTiraOQueSaiuDaLista() {
        val s = BoomSelection(setOf("a", "b"))
        s.retain(setOf("b", "c"))
        assertEquals(setOf("b"), s.selected)
        s.retain(setOf("b"))
        assertEquals(setOf("b"), s.selected)
    }

    @Test
    fun inOrderSegueAOrdemDaLista() {
        val s = BoomSelection()
        s.toggle("c")
        s.toggle("a")
        assertEquals(listOf("a", "c"), s.inOrder(listOf("a", "b", "c")))
    }

    @Test
    fun seMesmaEpocaVoltaAMesmaSelecaoEOutraVoltaVazia() {
        val scope = androidx.compose.runtime.saveable.SaverScope { true }
        val saved = with(BoomSelection.saver(epoch = 3)) { scope.save(BoomSelection(setOf("a", "b"))) }!!
        assertEquals(setOf("a", "b"), BoomSelection.saver(epoch = 3).restore(saved)!!.selected)
        assertEquals(emptySet<String>(), BoomSelection.saver(epoch = 4).restore(saved)!!.selected)
    }
}
