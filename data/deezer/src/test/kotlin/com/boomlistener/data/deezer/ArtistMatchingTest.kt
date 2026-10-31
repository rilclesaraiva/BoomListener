// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.deezer

import com.boomlistener.core.model.ExternalArtist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArtistMatchingTest {
    private fun artist(id: Long, name: String, fans: Long = 0) = ExternalArtist(id, name, null, fans, null)

    @Test
    fun tiraParticipacaoDoNome() {
        assertEquals("Djavan", primaryArtistName("Djavan feat. Caetano Veloso"))
        assertEquals("Anitta", primaryArtistName("Anitta ft. J Balvin"))
        assertEquals("Simon & Garfunkel", primaryArtistName("Simon & Garfunkel"))
        assertEquals("Earth, Wind & Fire", primaryArtistName("Earth, Wind & Fire"))
    }

    @Test
    fun nomeIgualIgnorandoAcento() {
        val picked = pickArtist("Joao Gilberto", listOf(artist(1, "João Gilberto Tribute"), artist(2, "João Gilberto")))
        assertEquals(2L, picked?.id)
    }

    @Test
    fun homonimosEscolheOMaisConhecido() {
        val picked = pickArtist("Nirvana", listOf(artist(1, "Nirvana", fans = 300), artist(2, "Nirvana", fans = 9_000_000)))
        assertEquals(2L, picked?.id)
    }

    @Test
    fun semNomeIgualNaoChuta() {
        assertNull(pickArtist("Chikoi The Maid", listOf(artist(1, "Chikoi"), artist(2, "The Maid"))))
    }

    @Test
    fun tituloComparavelDeAlbum() {
        assertEquals("abbey road", comparableTitle("Abbey Road (Remastered 2019)"))
        assertEquals("luz", comparableTitle("Luz - Single"))
        assertEquals("1989", comparableTitle("1989 [Deluxe Edition]"))
        assertEquals("acai", comparableTitle("Açaí - EP"))
    }

    @Test
    fun separaVariosArtistas() {
        assertEquals(listOf("Salako", "Ziahfyah", "ELHAE"), splitArtistNames("Salako, Ziahfyah y ELHAE"))
        assertEquals(listOf("Djavan", "Caetano Veloso"), splitArtistNames("Djavan feat. Caetano Veloso"))
        assertEquals(listOf("Anitta", "J Balvin"), splitArtistNames("Anitta & J Balvin"))
        assertEquals(listOf("Alok", "Vintage Culture"), splitArtistNames("Alok x Vintage Culture"))
        assertEquals(listOf("Malcolm X"), splitArtistNames("Malcolm X"))
        assertEquals(listOf("Chikoi The Maid"), splitArtistNames("Chikoi The Maid"))
    }

    @Test
    fun nomeInteiroPrimeiroDepoisCadaUm() {
        assertEquals(
            listOf("Earth, Wind & Fire", "Earth", "Wind", "Fire"),
            artistSearchNames("Earth, Wind & Fire"),
        )
        assertEquals(listOf("Djavan", "Caetano Veloso"), artistSearchNames("Djavan feat. Caetano Veloso"))
        assertEquals(listOf("Pitty"), artistSearchNames("Pitty"))
    }

    @Test
    fun juntaPrincipaisEConvidados() {
        assertEquals("Salako, Ziahfyah, ELHAE", joinArtists(listOf("Salako", "Ziahfyah", "ELHAE"), emptyList(), "Do Tha Thing"))
        assertEquals("Djavan feat. Caetano Veloso", joinArtists(listOf("Djavan"), listOf("Caetano Veloso"), "Linha do Equador"))
        // Convidado já no título não se repete.
        assertEquals("Anitta", joinArtists(listOf("Anitta"), listOf("J Balvin"), "Downtown (feat. J Balvin)"))
    }
}
