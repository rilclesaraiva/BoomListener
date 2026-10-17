// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialLogicTest {
    private val ana = SocialUser("a1", "ana", "Ana")

    private fun message(id: String, conv: String, at: Long, from: SocialUser? = ana) =
        ChatMessage(id, conv, from, MessageBody.Text("oi $id"), at)

    @Test
    fun arrobaValidoESugestao() {
        assertEquals("ana.clara", normalizeHandle(" @Ana.Clara "))
        assertTrue(isValidHandle("@rilcle_saraiva"))
        assertFalse(isValidHandle("ab"))
        assertFalse(isValidHandle("com espaço"))
        assertEquals("joao_ninguem", suggestHandle("João Ninguém"))
        assertEquals("a__", suggestHandle("Á"))
    }

    @Test
    fun conversaNovaContaNaoLidasEIgnoraRepetidas() {
        val first = mergeIncoming(emptyMap(), listOf(message("1", "dm:a1", 10), message("2", "dm:a1", 20)), null)
        val conv = first.getValue("dm:a1").conversation
        assertEquals("Ana", conv.title)
        assertEquals(2, conv.unread)
        assertEquals("2", conv.lastMessage?.id)

        val again = mergeIncoming(first, listOf(message("2", "dm:a1", 20)), null)
        assertEquals(2, again.getValue("dm:a1").messages.size)
        assertEquals(2, again.getValue("dm:a1").conversation.unread)
    }

    @Test
    fun conversaAbertaNaoAcumulaNaoLidas() {
        val merged = mergeIncoming(emptyMap(), listOf(message("1", "g:x", 10)), openConversationId = "g:x")
        assertEquals(0, merged.getValue("g:x").conversation.unread)
        assertTrue(merged.getValue("g:x").conversation.isGroup)
    }

    @Test
    fun ordemPorHoraEMaisRecentePrimeiroNaLista() {
        val merged = mergeIncoming(
            emptyMap(),
            listOf(message("b", "dm:a1", 30), message("a", "dm:a1", 10), message("c", "g:x", 50)),
            null,
        )
        assertEquals(listOf("a", "b"), merged.getValue("dm:a1").messages.map { it.id })
        assertEquals(listOf("g:x", "dm:a1"), sortConversations(merged.values.map { it.conversation }).map { it.id })
    }
}
