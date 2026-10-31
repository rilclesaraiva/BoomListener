// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

import com.boomlistener.core.model.ExternalArtist
import com.boomlistener.data.library.TrackGroup
import org.junit.Assert.assertEquals
import org.junit.Test

class ArtistSearchTest {
    private fun group(name: String) = TrackGroup(name.lowercase(), name, null, null, emptyList())
    private fun online(id: Long, name: String) = ExternalArtist(id, name, null, 0, null)

    private val mine = listOf(group("Djavan"), group("Céu"), group("Gal Costa"))

    @Test
    fun buscaLocalIgnoraAcentoEMaiuscula() {
        assertEquals(listOf("Céu"), matchingLocalArtists(mine, "ceu").map { it.name })
        assertEquals(listOf("Gal Costa"), matchingLocalArtists(mine, "COSTA").map { it.name })
        assertEquals(mine, matchingLocalArtists(mine, "  "))
    }

    @Test
    fun doDeezerSoQuemNaoEstaNoAparelho() {
        val found = listOf(online(1, "Céu"), online(2, "Ceumar"), online(3, "CEU"), online(2, "Ceumar"))
        assertEquals(listOf(2L), onlineArtistsNotOwned(found, mine).map { it.id })
    }
}
