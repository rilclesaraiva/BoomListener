// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import com.boomlistener.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

internal fun track(id: Long, title: String, artist: String? = null, album: String? = null) = Track(
    id = id,
    uri = "content://media/external/audio/media/$id",
    title = title,
    artist = artist,
    album = album,
    albumId = 0,
    durationMs = 200_000,
    trackNumber = null,
    year = null,
    dateAddedSeconds = 0,
)

class LibraryLogicTest {
    private val tracks = listOf(
        track(1, "Açaí", artist = "Djavan", album = "Luz"),
        track(2, "Garota de Ipanema", artist = "João Gilberto"),
        track(3, "Sem artista"),
    )

    @Test
    fun buscaVaziaMostraTudo() {
        assertEquals(tracks, filterTracks(tracks, "  "))
    }

    @Test
    fun buscaIgnoraAcentoEMaiuscula() {
        assertEquals(listOf(1L), filterTracks(tracks, "ACAI").map { it.id })
        assertEquals(listOf(2L), filterTracks(tracks, "joao").map { it.id })
    }

    @Test
    fun buscaPorAlbum() {
        assertEquals(listOf(1L), filterTracks(tracks, "luz").map { it.id })
    }

    @Test
    fun duracaoFormatada() {
        assertEquals("3:35", formatDuration(215_000))
        assertEquals("0:05", formatDuration(5_400))
        assertEquals("1:02:05", formatDuration(3_725_000))
    }
}
