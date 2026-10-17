// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.library

import com.boomlistener.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryCollectionsTest {
    private var nextId = 1L
    private fun track(
        title: String,
        artist: String? = null,
        album: String? = null,
        albumId: Long = 0,
        genre: String? = null,
        number: Int? = null,
    ) = Track(nextId++, "content://$title", title, artist, album, albumId, 1000, number, null, 0, genre)

    @Test
    fun generoSemDiferencaDeMaiusculaEComVarios() {
        val groups = groupByGenre(
            listOf(
                track("a", genre = "Rock"),
                track("b", genre = "rock"),
                track("c", genre = "Rock; MPB"),
                track("d", genre = null),
            ),
        )
        assertEquals(listOf("MPB", "Rock"), groups.map { it.name })
        assertEquals(3, groups[1].tracks.size)
    }

    @Test
    fun artistaPrincipalComAcento() {
        val groups = groupByArtist(
            listOf(track("a", artist = "Djavan"), track("b", artist = "Djavan feat. Caetano Veloso"), track("c", artist = "Ávila")),
        )
        assertEquals(listOf("Ávila", "Djavan"), groups.map { it.name })
        assertEquals(2, groups[1].tracks.size)
    }

    @Test
    fun albumEmOrdemDeFaixa() {
        val groups = groupByAlbum(
            listOf(
                track("Oceano", "Djavan", "Djavan", albumId = 7, number = 2),
                track("Açaí", "Djavan", "Luz", albumId = 9, number = 3),
                track("Samurai", "Djavan", "Luz", albumId = 9, number = 1),
                track("Solta", null, null, albumId = 5),
            ),
        )
        assertEquals(listOf("Djavan", "Luz"), groups.map { it.name })
        assertEquals(listOf("Samurai", "Açaí"), groups[1].tracks.map { it.title })
        assertEquals("Djavan", groups[1].subtitle)
    }
}
