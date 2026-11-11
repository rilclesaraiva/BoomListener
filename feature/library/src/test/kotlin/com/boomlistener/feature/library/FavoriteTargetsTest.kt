// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import org.junit.Assert.assertEquals
import org.junit.Test

class FavoriteTargetsTest {
    @Test
    fun misturadasFavoritaSoAsQueFaltam() {
        assertEquals(listOf("b", "c"), favoriteTargets(listOf("a", "b", "c"), setOf("a")))
    }

    @Test
    fun todasFavoritasTiraTodas() {
        assertEquals(listOf("a", "b"), favoriteTargets(listOf("a", "b"), setOf("a", "b", "x")))
    }

    @Test
    fun nenhumaFavoritaFavoritaTodas() {
        assertEquals(listOf("a", "b"), favoriteTargets(listOf("a", "b"), emptySet()))
    }

    @Test
    fun listaVaziaNaoFazNada() {
        assertEquals(emptyList<String>(), favoriteTargets(emptyList(), setOf("a")))
    }
}
