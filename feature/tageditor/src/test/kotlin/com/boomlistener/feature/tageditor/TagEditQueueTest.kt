// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.tageditor

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TagEditQueueTest {
    @After
    fun tearDown() = TagEditQueue.clear()

    @Test
    fun umaMusicaSoNaoFormaFila() {
        TagEditQueue.start(listOf("a"))
        assertNull(TagEditQueue.state.value)
    }

    @Test
    fun repetidasSaemDaFila() {
        TagEditQueue.start(listOf("a", "b", "a"))
        assertEquals(2, TagEditQueue.state.value?.size)
    }

    @Test
    fun avancaAteOFimEEncerra() {
        TagEditQueue.start(listOf("a", "b", "c"))
        assertEquals("a", TagEditQueue.state.value?.current)
        assertEquals(1, TagEditQueue.state.value?.position)
        assertEquals("b", TagEditQueue.advance())
        assertEquals(2, TagEditQueue.state.value?.position)
        assertEquals("c", TagEditQueue.advance())
        assertNull(TagEditQueue.advance())
        assertNull(TagEditQueue.state.value)
    }

    @Test
    fun clearEncerra() {
        TagEditQueue.start(listOf("a", "b"))
        TagEditQueue.clear()
        assertNull(TagEditQueue.state.value)
        assertNull(TagEditQueue.advance())
    }
}
