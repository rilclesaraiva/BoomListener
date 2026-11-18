// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.library

import com.boomlistener.core.model.Playlist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistOpsTest {
    @Test
    fun adicionarPulaAsQueJaEstao() {
        val (list, added) = addUnique(listOf("a", "b"), listOf("b", "c", "c", "d"))
        assertEquals(listOf("a", "b", "c", "d"), list)
        assertEquals(2, added)
    }

    @Test
    fun tirarMusicasDaPlaylist() {
        val (list, removed) = removeAll(listOf("a", "b", "c", "b"), listOf("b", "z"))
        assertEquals(listOf("a", "c"), list)
        assertEquals(2, removed)
        assertEquals(listOf("a") to 0, removeAll(listOf("a"), emptyList()))
    }

    @Test
    fun moverParaCimaEParaBaixo() {
        assertEquals(listOf("c", "a", "b"), moveItem(listOf("a", "b", "c"), 2, 0))
        assertEquals(listOf("b", "c", "a"), moveItem(listOf("a", "b", "c"), 0, 2))
        assertEquals(listOf("a", "b"), moveItem(listOf("a", "b"), 0, 5)) // fora da lista: nada muda
    }

    @Test
    fun nomeLimpoOuPadrao() {
        assertEquals("Para correr", cleanName("  Para   correr ", "x"))
        assertEquals("Playlist 3", newPlaylist(List(2) { newPlaylist(emptyList(), "p$it", emptyList(), 1) }, " ", emptyList(), 5).name)
    }

    @Test
    fun idsNuncaRepetem() {
        val first = newPlaylist(emptyList(), "a", emptyList(), nowMs = 100)
        val second = newPlaylist(listOf(first), "b", emptyList(), nowMs = 100) // mesmo instante
        assertTrue(second.id > first.id)
    }

    @Test
    fun playlistTravadaVoltaTravadaEArquivoAntigoAbreDestravado() {
        val travada = Playlist(1, "Wrapped 2026", listOf("a", "b"), 10, 20, locked = true)
        val comum = Playlist(2, "Minha", listOf("c"), 11, 21)
        val lidas = parsePlaylists(playlistsToJson(listOf(travada, comum)))
        assertEquals(listOf(travada, comum), lidas)
        // Arquivo gravado antes do campo existir: nada vem travado.
        val antigo = """[{"id":3,"name":"Velha","tracks":["x"],"created":1,"updated":2}]"""
        assertFalse(parsePlaylists(antigo).single().locked)
        // A playlist comum não ganha o campo no arquivo.
        assertFalse(playlistsToJson(listOf(comum)).contains("locked"))
    }

    @Test
    fun playlistTravadaNaoRenomeiaNemRecebeMusicas() {
        val travada = Playlist(1, "Wrapped 2026", listOf("a"), 10, 20, locked = true)
        assertEquals(travada, renamed(travada, "Outro nome"))
        assertEquals(travada to 0, addedTo(travada, listOf("b", "c")))
        val comum = travada.copy(locked = false)
        assertEquals("Outro nome", renamed(comum, " Outro  nome ").name)
        val (depois, entraram) = addedTo(comum, listOf("a", "b"))
        assertEquals(listOf("a", "b"), depois.trackUris)
        assertEquals(1, entraram)
    }
}
