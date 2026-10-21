// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import com.boomlistener.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class FavoriteTracksTest {
    private fun track(id: Long) = Track(id, "content://$id", "Música $id", "Djavan", "Luz", 1, 200_000, 1, 1982, 0)

    @Test
    fun naOrdemDosFavoritosSemAsQueSairamDoAparelho() {
        val library = listOf(track(1), track(2), track(3))
        assertEquals(listOf(3L, 1L), favoriteTracks(listOf("content://3", "content://9", "content://1"), library).map { it.id })
    }
}
