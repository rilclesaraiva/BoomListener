// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.library

import com.boomlistener.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun t(id: Long, title: String, artist: String?, durationMs: Long = 200_000, album: String? = null) = Track(
    id = id, uri = "content://media/$id", title = title, artist = artist, album = album, albumId = 0,
    durationMs = durationMs, trackNumber = null, year = null, dateAddedSeconds = 0,
)

private fun ids(groups: List<DuplicateGroup>) = groups.map { g -> g.tracks.map { it.id }.sorted() }

class DuplicateFinderTest {

    @Test
    fun ignoraAcentoMaiusculaEPontuacao() {
        val groups = findDuplicateGroups(listOf(t(1, "Açaí", "Djavan"), t(2, "ACAI!", "djavan")))
        assertEquals(listOf(listOf(1L, 2L)), ids(groups))
    }

    @Test
    fun featNoTituloNaoImpedeOGrupo() {
        val groups = findDuplicateGroups(
            listOf(
                t(1, "Song", "Djavan"),
                t(2, "Song (feat. Caetano)", "Djavan"),
                t(3, "Song [ft. X]", "Djavan"),
                t(4, "Song - feat. Y", "Djavan"),
            ),
        )
        assertEquals(listOf(listOf(1L, 2L, 3L, 4L)), ids(groups))
    }

    @Test
    fun featNoArtistaNaoImpedeOGrupo() {
        val groups = findDuplicateGroups(listOf(t(1, "Song", "Djavan"), t(2, "Song", "Djavan feat. Caetano")))
        assertEquals(listOf(listOf(1L, 2L)), ids(groups))
    }

    @Test
    fun homonimosDeArtistasDiferentesNaoSaoRepetidas() {
        assertTrue(findDuplicateGroups(listOf(t(1, "Home", "Artist A"), t(2, "Home", "Artist B"))).isEmpty())
    }

    @Test
    fun versoesDiferentesNaoSaoRepetidas() {
        val groups = findDuplicateGroups(
            listOf(
                t(1, "Song", "Band"),
                t(2, "Song (Live)", "Band"),
                t(3, "Song (Remix)", "Band"),
                t(4, "Song (Acoustic)", "Band"),
            ),
        )
        assertTrue(groups.isEmpty())
    }

    @Test
    fun duracaoMuitoDiferenteNaoEhRepetida() {
        assertTrue(findDuplicateGroups(listOf(t(1, "Song", "Band", 200_000), t(2, "Song", "Band", 230_000))).isEmpty())
    }

    @Test
    fun duracaoParecidaDentroDaToleranciaEhRepetida() {
        val groups = findDuplicateGroups(listOf(t(1, "Song", "Band", 200_000), t(2, "Song", "Band", 202_900)))
        assertEquals(listOf(listOf(1L, 2L)), ids(groups))
    }

    @Test
    fun mesmoNomeComDuasDuracoesGeraDoisGrupos() {
        val groups = findDuplicateGroups(
            listOf(
                t(1, "Song", "Band", 200_000), t(2, "Song", "Band", 201_000),
                t(3, "Song", "Band", 300_000), t(4, "Song", "Band", 300_500),
            ),
        )
        assertEquals(listOf(listOf(1L, 2L), listOf(3L, 4L)), ids(groups).sortedBy { it.first() })
    }

    @Test
    fun semArtistaOuSemDuracaoNuncaEntra() {
        val groups = findDuplicateGroups(
            listOf(
                t(1, "Song", null), t(2, "Song", null),
                t(3, "Song", "  "), t(4, "Song", ""),
                t(5, "Other", "Band", 0), t(6, "Other", "Band", 0),
            ),
        )
        assertTrue(groups.isEmpty())
    }

    @Test
    fun mesmaMusicaEmAlbumEColetaneaEhRepetida() {
        val groups = findDuplicateGroups(
            listOf(t(1, "Song", "Band", album = "Album"), t(2, "Song", "Band", album = "Greatest Hits")),
        )
        assertEquals(listOf(listOf(1L, 2L)), ids(groups))
    }

    @Test
    fun faixaUnicaNaoFormaGrupo() {
        assertTrue(findDuplicateGroups(listOf(t(1, "Song", "Band"))).isEmpty())
    }

    @Test
    fun nomesSoDePontuacaoNaoCasam() {
        assertTrue(findDuplicateGroups(listOf(t(1, "???", "Band"), t(2, "!!!", "Band"))).isEmpty())
    }
}
