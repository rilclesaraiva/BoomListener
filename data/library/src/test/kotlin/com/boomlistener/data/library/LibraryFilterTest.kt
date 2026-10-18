// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryFilterTest {

    private val music = "/storage/emulated/0/Music"

    @Test
    fun semFiltroTudoEntra() {
        assertTrue(LibraryFilter().accepts(1_000, null))
        assertTrue(LibraryFilter().accepts(0, "$music/Rock"))
    }

    @Test
    fun duracaoMinimaEscondeAudiosCurtos() {
        val filter = LibraryFilter(minDurationSeconds = 30)
        assertFalse(filter.accepts(12_000, music))
        assertFalse(filter.accepts(29_999, music))
        assertTrue(filter.accepts(30_000, music))
        assertTrue(filter.accepts(240_000, music))
    }

    @Test
    fun duracaoDesconhecidaNaoSome() {
        assertTrue(LibraryFilter(minDurationSeconds = 60).accepts(0, music))
    }

    @Test
    fun pastasEscolhidasIncluemSubpastasMasNaoNomesParecidos() {
        val filter = LibraryFilter(onlyChosenFolders = true, folders = setOf("$music/Rock"))
        assertTrue(filter.accepts(200_000, "$music/Rock"))
        assertTrue(filter.accepts(200_000, "$music/Rock/Álbum ao vivo"))
        assertFalse(filter.accepts(200_000, "$music/Rockabilly"))
        assertFalse(filter.accepts(200_000, "/storage/emulated/0/WhatsApp/Media/WhatsApp Audio"))
        assertFalse(filter.accepts(200_000, null))
    }

    @Test
    fun modoPastasSemNenhumaEscolhidaNaoMostraNada() {
        assertFalse(LibraryFilter(onlyChosenFolders = true).accepts(200_000, music))
    }

    @Test
    fun listaDePastasGuardadaNaoValeNoModoTodas() {
        val filter = LibraryFilter(onlyChosenFolders = false, folders = setOf("$music/Rock"))
        assertTrue(filter.accepts(200_000, "/storage/emulated/0/Download"))
    }

    @Test
    fun pastaVemDoCaminhoCompletoOuDoRelativo() {
        assertEquals("$music/Rock", parentFolder("$music/Rock/faixa.mp3", "Music/Rock/"))
        assertEquals("Music/Rock", parentFolder(null, "Music/Rock/"))
        assertNull(parentFolder(null, null))
        assertNull(parentFolder(null, "  "))
    }

    @Test
    fun localDaPastaSemOComecoDoArmazenamento() {
        assertEquals("Music/Rock", folderLocation("$music/Rock"))
        assertEquals("Download", folderLocation("/storage/emulated/10/Download"))
        assertEquals("1A2B-3C4D/Música", folderLocation("/storage/1A2B-3C4D/Música"))
        assertEquals("Music", folderLocation("Music"))
    }

    @Test
    fun agrupaPastasComContagemEEmOrdem() {
        val folders = groupFolders(
            listOf("$music/Rock", "/storage/emulated/0/Download", "$music/Rock", null, "$music/Ávila"),
        )
        assertEquals(listOf("Download", "Music/Rock", "Music/Ávila"), folders.map { it.location })
        assertEquals(2, folders.first { it.name == "Rock" }.songs)
        assertEquals("Download", folders.first().name)
    }
}
