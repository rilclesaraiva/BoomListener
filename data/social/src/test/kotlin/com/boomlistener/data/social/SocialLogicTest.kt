// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialLogicTest {
    private val ana = SocialUser("a1", "ana", "Ana")

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

    private fun person(id: String, now: String?, relation: Relation = Relation.Friend, lastAt: Long? = null) = FriendProfile(
        user = SocialUser(id, id, id.uppercase()),
        sharing = now != null || lastAt != null,
        now = now?.let { TrackInfo(it, null, null) },
        updatedMs = null,
        recent = lastAt?.let { listOf(ListenedTrack(TrackInfo("Antiga", null, null), it)) }.orEmpty(),
        blockedByMe = false,
        relation = relation,
    )

    @Test
    fun amigosAoVivo() {
        val start = listOf(person("a", "Oceano"), person("b", null))
        val changed = withPerson(start, person("a", "Flor de Lis"))
        assertEquals(listOf("a", "b"), changed.map { it.user.id }) // mantém a posição
        assertEquals("Flor de Lis", changed[0].now?.title)
        // Parou de compartilhar continua amigo; desfez a amizade sai da lista.
        assertEquals(listOf("a", "b"), withPerson(start, person("a", null)).map { it.user.id })
        assertEquals(listOf("b"), withPerson(start, person("a", null, Relation.None)).map { it.user.id })
        assertEquals(listOf("a", "b", "c"), withPerson(start, person("c", "X")).map { it.user.id })
    }

    @Test
    fun ordemDosAmigos() {
        val list = listOf(
            person("zeca", null),
            person("ana", null, lastAt = 100),
            person("beto", "Agora"),
            person("caio", null, lastAt = 500),
        )
        assertEquals(listOf("beto", "caio", "ana", "zeca"), sortFriends(list).map { it.user.id })
        assertEquals("Antiga", list[1].lastTrack?.title)
    }

    @Test
    fun recomendacoesSemRepetirNoMaximo50E30Dias() {
        val now = 100L * 86_400_000L
        fun rec(id: String, daysAgo: Long) =
            Recommendation(id, ana, MusicCard(MusicKind.Track, "M$id", null, null, null, null), now - daysAgo * 86_400_000L)
        val stored = listOf(rec("1", 1).copy(seen = true), rec("velha", 31))
        val merged = mergeRecommendations(stored, listOf(rec("1", 1), rec("2", 0)), now)
        assertEquals(listOf("2", "1"), merged.map { it.id })
        assertTrue(merged[1].seen) // a guardada (já vista) não é trocada pela repetida
        val many = (1..60).map { rec("n$it", 0).copy(atMs = now - it) }
        assertEquals(50, mergeRecommendations(emptyList(), many, now).size)
    }
}
