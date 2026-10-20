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

    @Test
    fun corDoPerfil() {
        assertEquals("#FF5500", cleanProfileColor("#ff5500"))
        assertEquals("#FF5500", cleanProfileColor(" FF5500 "))
        assertEquals("", cleanProfileColor(""))
        assertEquals(null, cleanProfileColor("red"))
        assertEquals(null, cleanProfileColor("#FFF"))
    }

    @Test
    fun esperaParaReconectar() {
        assertEquals(3_000L, reconnectDelayMs(1, 0.5))
        assertEquals(60_000L, reconnectDelayMs(2, 0.5))
        assertEquals(120_000L, reconnectDelayMs(3, 0.5))
        assertEquals(480_000L, reconnectDelayMs(5, 0.5))
        assertEquals(900_000L, reconnectDelayMs(6, 0.5)) // teto de 15 minutos
        assertEquals(900_000L, reconnectDelayMs(40, 0.5))
        // Sorteio de ±20%.
        assertEquals(48_000L, reconnectDelayMs(2, 0.0))
        assertEquals(72_000L, reconnectDelayMs(2, 1.0))
    }

    @Test
    fun socialDescansando() {
        val now = 1_760_000_000_000L // algum momento do dia
        val midnight = nextUtcMidnight(now)
        assertEquals(0L, midnight % 86_400_000L)
        assertTrue(midnight > now && midnight - now <= 86_400_000L)
        assertEquals(midnight + 5, restingUntil(503, "resting", midnight + 5, now))
        assertEquals(midnight, restingUntil(503, "resting", 0, now))
        assertEquals(midnight, restingUntil(429, null, 0, now)) // limite da própria Cloudflare
        assertEquals(null, restingUntil(429, "slow_down", 0, now)) // só a cota por minuto
        assertEquals(null, restingUntil(503, "unavailable", 0, now))
        assertEquals(null, restingUntil(200, null, 0, now))
    }

    @Test
    fun ouvindoAgoraAoVivo() {
        fun person(id: String, sharing: Boolean, now: String?) = FriendProfile(
            user = SocialUser(id, id, id),
            sharing = sharing,
            now = now?.let { TrackInfo(it, null, null) },
            updatedMs = null,
            recent = emptyList(),
            blockedByMe = false,
        )
        val start = listOf(person("a", true, "Oceano"), person("b", true, null))
        val changed = withPerson(start, person("a", true, "Flor de Lis"))
        assertEquals(listOf("a", "b"), changed.map { it.user.id }) // mantém a posição
        assertEquals("Flor de Lis", changed[0].now?.title)
        assertEquals(listOf("b"), withPerson(start, person("a", false, null)).map { it.user.id })
        assertEquals(listOf("a", "b", "c"), withPerson(start, person("c", true, "X")).map { it.user.id })
    }

    @Test
    fun estadoDaMensagemDepoisDeEnviar() {
        assertEquals(null, deliveryAfterSend("live")) // falta o recibo
        assertEquals(Delivery.Waiting, deliveryAfterSend("waiting"))
        assertEquals(Delivery.Mailbox, deliveryAfterSend("mailbox"))
        assertEquals(Delivery.Sent, deliveryAfterSend("group"))
        assertEquals(Delivery.Sent, deliveryAfterSend("")) // servidor antigo
    }

    @Test
    fun estadoSoAndaParaFrente() {
        assertTrue(advances(Delivery.Waiting, Delivery.Delivered))
        assertTrue(advances(Delivery.Waiting, Delivery.Mailbox))
        assertTrue(advances(Delivery.Mailbox, Delivery.Delivered))
        assertFalse(advances(Delivery.Delivered, Delivery.Waiting))
        assertFalse(advances(Delivery.Delivered, Delivery.Mailbox))
        assertFalse(advances(Delivery.Waiting, Delivery.Waiting))
    }
}
