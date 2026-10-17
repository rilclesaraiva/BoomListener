// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.lastfm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GenrePickingTest {
    @Test
    fun pulaOpiniaoEpocaENomeDoArtista() {
        val tags = listOf("seen live", "Chikoi The Maid", "2020s", "favorites", "math rock", "japanese")
        assertEquals("Math Rock", pickGenre(tags, artistName = "Chikoi The Maid"))
    }

    @Test
    fun siglasFicamEmMaiuscula() {
        assertEquals("MPB", pickGenre(listOf("mpb", "brazilian")))
        assertEquals("R&B", pickGenre(listOf("rnb")))
    }

    @Test
    fun hifenECapitalizacao() {
        assertEquals("J-Pop", prettyGenre("j-pop"))
        assertEquals("Indie Rock", prettyGenre("INDIE ROCK"))
    }

    @Test
    fun semGeneroDeVerdade() {
        assertNull(pickGenre(listOf("love", "90s", "female vocalists")))
        assertNull(pickGenre(emptyList()))
    }
}
