// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.tags

import com.boomlistener.core.model.TrackTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TagGapsTest {
    private val complete = TrackTags(
        title = "Açaí", artist = "Djavan", album = "Luz", albumArtist = "Djavan", year = "1982",
        genre = "MPB", trackNumber = "3", composer = "Djavan", lyrics = "letra",
    )

    @Test
    fun completaComCapaNaoFaltaNada() {
        assertTrue(gapsOf(complete, hasCover = true).isEmpty())
    }

    @Test
    fun semNadaFaltaTudo() {
        assertEquals(TagGap.entries.toSet(), gapsOf(TrackTags(title = "x"), hasCover = false))
    }

    @Test
    fun espacosContamComoVazio() {
        val gaps = gapsOf(complete.copy(artist = "  ", genre = ""), hasCover = true)
        assertEquals(setOf(TagGap.Artist, TagGap.Genre), gaps)
    }

    @Test
    fun soACapaFalta() {
        assertEquals(setOf(TagGap.Cover), gapsOf(complete, hasCover = false))
    }

    @Test
    fun saiNaOrdemDoEnum() {
        val gaps = gapsOf(TrackTags(), hasCover = false).toList()
        assertEquals(TagGap.entries.toList(), gaps)
    }
}
