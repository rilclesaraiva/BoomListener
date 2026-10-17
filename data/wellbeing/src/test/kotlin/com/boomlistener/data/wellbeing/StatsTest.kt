// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.wellbeing

import org.junit.Assert.assertEquals
import org.junit.Test

class StatsTest {
    @Test
    fun somaReproducoesERanqueia() {
        var stats = emptyMap<String, SongStat>()
        stats = applyPlay(stats, "a", "Açaí", "Djavan", "Luz", 1)
        stats = applyPlay(stats, "b", "Oceano", "Djavan", null, 2)
        stats = applyPlay(stats, "a", "Açaí", "Djavan", "Luz", 3)
        val top = rankSongs(stats, 10)
        assertEquals(listOf("a", "b"), top.map { it.uri })
        assertEquals(2, top.first().plays)
    }

    @Test
    fun empateDesempataPelaMaisRecente() {
        var stats = emptyMap<String, SongStat>()
        stats = applyPlay(stats, "a", "A", null, null, 10)
        stats = applyPlay(stats, "b", "B", null, null, 20)
        assertEquals(listOf("b", "a"), rankSongs(stats, 10).map { it.uri })
        assertEquals(1, rankSongs(stats, 1).size)
    }
}
