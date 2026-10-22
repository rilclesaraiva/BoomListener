// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.releases

import com.boomlistener.core.model.Release
import com.boomlistener.core.model.ReleaseType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseAlertsTest {
    private val today = LocalDate.of(2026, 10, 22)
    private fun release(id: Long, date: String?) = Release(id, "R$id", ReleaseType.Single, date, null, null)

    @Test
    fun artistaRecemSeguidoNaoAvisaNada() {
        val check = findNewReleases(setOf(1), emptyMap(), mapOf(1L to listOf(release(10, "2026-10-20"))), today)
        assertTrue(check.fresh.isEmpty())
        assertEquals(setOf(10L), check.known[1L])
    }

    @Test
    fun lancamentoNovoERecenteAvisa() {
        val known = mapOf(1L to setOf(10L))
        val fetched = mapOf(1L to listOf(release(10, "2026-09-01"), release(11, "2026-10-21")))
        val check = findNewReleases(setOf(1), known, fetched, today)
        assertEquals(listOf(11L), check.fresh.map { it.release.id })
        assertEquals(setOf(10L, 11L), check.known[1L])
    }

    @Test
    fun antigoQueApareceuAgoraOuFuturoNaoAvisaMasViraConhecido() {
        val known = mapOf(1L to setOf(10L))
        val fetched = mapOf(1L to listOf(release(12, "2019-05-01"), release(13, "2026-12-01"), release(14, null)))
        val check = findNewReleases(setOf(1), known, fetched, today)
        assertTrue(check.fresh.isEmpty())
        assertEquals(setOf(10L, 12L, 13L, 14L), check.known[1L])
    }

    @Test
    fun falhaMantemOConhecidoEDeixarDeSeguirApaga() {
        val known = mapOf(1L to setOf(10L), 2L to setOf(20L))
        // Artista 1 falhou (sem discografia); artista 2 não é mais seguido.
        val check = findNewReleases(setOf(1), known, emptyMap(), today)
        assertEquals(mapOf(1L to setOf(10L)), check.known)
    }

    @Test
    fun guardarELerOConhecido() {
        val known = mapOf(1L to setOf(10L, 11L), 2L to emptySet())
        assertEquals(known, decodeKnown(encodeKnown(known)))
    }
}
