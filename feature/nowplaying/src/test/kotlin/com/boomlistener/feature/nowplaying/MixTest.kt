// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import com.boomlistener.core.model.Track
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MixTest {
    private fun track(id: Long, artist: String? = null, genre: String? = null) = Track(
        id = id, uri = "u$id", title = "t$id", artist = artist, album = null, albumId = 0,
        durationMs = 1000, trackNumber = null, year = null, dateAddedSeconds = 0, genre = genre,
    )

    @Test
    fun `mix has every other song once and never the current one`() {
        val library = (1L..30L).map { track(it) } + track(5)
        val mix = buildMix("u1", library, Random(7))
        assertFalse(mix.any { it.uri == "u1" })
        assertEquals(29, mix.size)
        assertEquals(mix.size, mix.map { it.uri }.toSet().size)
    }

    @Test
    fun `related songs come first, ignoring case and spaces`() {
        val library = listOf(track(1, artist = "Chikoi The Maid")) +
            (2L..20L).map { track(it, artist = "Other") } +
            listOf(track(21, artist = " chikoi the maid "), track(22, genre = "Rock"))
        val mix = buildMix("u1", library, Random(3))
        assertEquals("u21", mix.first().uri)
    }

    @Test
    fun `at most five related songs open the mix`() {
        val library = (1L..20L).map { track(it, genre = "Jersey Club") } + (21L..40L).map { track(it, genre = "Rock") }
        val mix = buildMix("u1", library, Random(1))
        assertTrue(mix.take(MIX_RELATED_FIRST).all { it.genre == "Jersey Club" })
        assertEquals(39, mix.size)
    }

    @Test
    fun `unknown current song gives the whole library shuffled`() {
        val library = (1L..10L).map { track(it) }
        assertEquals(10, buildMix("nope", library, Random(2)).size)
    }
}
