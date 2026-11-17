// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import com.boomlistener.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class SelectedGroupsTest {
    private fun track(id: Long, artist: String = "A") = Track(id, "content://$id", "t$id", artist, "al", 1, 1000, 1, 2020, 0)

    @Test
    fun musicaEmDoisGruposEntraUmaVezSo() {
        val album = listOf(track(1), track(2))
        val artist = listOf(track(2), track(3, "Beyoncé feat. Jay-Z"))
        assertEquals(listOf(1L, 2L, 3L), distinctTracks(listOf(album, artist)).map { it.id })
    }

    @Test
    fun semGruposNaoTemMusicas() {
        assertEquals(emptyList<Track>(), distinctTracks(emptyList()))
        assertEquals(emptyList<Track>(), distinctTracks(listOf(emptyList(), emptyList())))
    }

    @Test
    fun mantemAOrdemDosGrupos() {
        val first = listOf(track(5), track(4))
        val second = listOf(track(1))
        assertEquals(listOf(5L, 4L, 1L), distinctTracks(listOf(first, second)).map { it.id })
    }
}
