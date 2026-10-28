// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import com.boomlistener.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class MosaicCoversTest {
    private fun song(id: Long, albumId: Long) = Track(
        id = id, uri = "content://$id", title = "Música $id", artist = "Artista", album = "Álbum $albumId",
        albumId = albumId, durationMs = 1000, trackNumber = null, year = null, dateAddedSeconds = 0,
    )

    @Test
    fun umaCapaPorAlbumSemRepetir() {
        // Só uma música dos Mamonas: a capa deles aparece uma vez só.
        val tracks = listOf(song(1, 10), song(2, 20), song(3, 20), song(4, 30))
        assertEquals(listOf(1L, 2L, 4L), mosaicCovers(tracks).map { it.id })
    }

    @Test
    fun quatroAlbunsOuMaisMontaO2x2() {
        val tracks = listOf(song(1, 10), song(2, 10), song(3, 20), song(4, 30), song(5, 40), song(6, 50))
        assertEquals(listOf(1L, 3L, 4L, 5L), mosaicCovers(tracks).map { it.id })
    }

    @Test
    fun duasMusicasDeDoisAlbunsViramDuasMetades() {
        val tracks = listOf(song(1, 10), song(2, 20))
        assertEquals(listOf(1L, 2L), mosaicCovers(tracks).map { it.id })
    }

    @Test
    fun todasDoMesmoAlbumFicaComCapaUnica() {
        val tracks = listOf(song(1, 10), song(2, 10), song(3, 10), song(4, 10))
        assertEquals(listOf(1L), mosaicCovers(tracks).map { it.id })
    }

    @Test
    fun listaVaziaNaoTemCapa() {
        assertEquals(emptyList<Track>(), mosaicCovers(emptyList()))
    }
}
