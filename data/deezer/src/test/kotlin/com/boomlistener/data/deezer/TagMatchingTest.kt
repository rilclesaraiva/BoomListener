// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.deezer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TagMatchingTest {

    private fun candidate(
        id: Long,
        title: String,
        artist: String,
        album: String = "Álbum",
        durationMs: Long = 200_000,
    ) = TrackCandidate(id, albumId = id * 10, title, artist, album, coverUrl = null, durationMs, link = null)

    @Test
    fun tituloComparavelIgnoraEnfeites() {
        assertEquals("acai", comparableSongTitle("03 - Açaí [Official Audio]"))
        assertEquals("oceano", comparableSongTitle("Oceano (feat. Caetano Veloso)"))
        assertEquals("nao e proibido", comparableSongTitle("Não É Proibido!"))
    }

    @Test
    fun mesmaMusicaComAcentoDiferenteBateAlto() {
        val local = LocalTrackInfo("Acai", "Djavan", "Luz", 200_000)
        val confidence = matchConfidence(local, candidate(1, "Açaí", "Djavan", "Luz", 201_000))
        assertTrue(confidence >= AUTO_PICK_CONFIDENCE)
    }

    @Test
    fun artistaHomonimoDeTituloFicaDeFora() {
        // Duas músicas chamadas "Catharsis", de artistas diferentes.
        val local = LocalTrackInfo("Catharsis", "Chikoi The Maid", null, 180_000)
        val ranked = rankCandidates(
            local,
            listOf(candidate(1, "Catharsis", "Outra Banda", durationMs = 180_000), candidate(2, "Catharsis", "Chikoi The Maid", durationMs = 181_000)),
        )
        assertEquals(listOf(2L), ranked.map { it.trackId })
    }

    @Test
    fun featNoArtistaAindaBate() {
        val local = LocalTrackInfo("Envolver", "Anitta feat. Alguém", null, 193_000)
        val confidence = matchConfidence(local, candidate(1, "Envolver", "Anitta", durationMs = 193_500))
        assertTrue(confidence >= AUTO_PICK_CONFIDENCE)
    }

    @Test
    fun versaoAoVivoComDuracaoMuitoDiferenteFicaAbaixo() {
        val local = LocalTrackInfo("Oceano", "Djavan", null, 260_000)
        val studio = matchConfidence(local, candidate(1, "Oceano", "Djavan", durationMs = 259_000))
        val live = matchConfidence(local, candidate(2, "Oceano (Ao Vivo)", "Djavan", durationMs = 330_000))
        assertTrue(studio > live)
        assertTrue(live < AUTO_PICK_CONFIDENCE)
    }

    @Test
    fun semArtistaNoArquivoAindaAchaPeloTituloEDuracao() {
        val local = LocalTrackInfo("Conviction", null, null, 243_000)
        val confidence = matchConfidence(local, candidate(1, "Conviction", "Chikoi The Maid", durationMs = 243_000))
        assertTrue(confidence >= MIN_CONFIDENCE)
    }

    @Test
    fun rankingSemRepetirNoMesmoAlbum() {
        val local = LocalTrackInfo("Blink", "Chikoi The Maid", null, 200_000)
        val same = candidate(1, "Blink", "Chikoi The Maid")
        val ranked = rankCandidates(local, listOf(same, same.copy(trackId = 99)))
        assertEquals(1, ranked.size)
    }

    @Test
    fun remixDoArtistaComOutroArtistaPrincipalApareceNasMusicasDele() {
        // "Vertigo (Lover Girl Remix)" do Dxrkaii: no Deezer o artista principal é outro.
        val local = LocalTrackInfo("Vertigo (Lover Girl Remix)", "Dxrkaii", "Vertigo (Lover Girl Remix)", 127_000)
        val tracks = listOf(
            candidate(1, "Vertigo (Lover Girl Remix)", "Outra Pessoa", durationMs = 127_000),
            candidate(2, "New Jeans (Jersey Club)", "Dxrkaii", durationMs = 110_000),
        )
        // Pela busca comum, o artista diferente derruba a música.
        assertTrue(rankCandidates(local, tracks).isEmpty())
        // Nas músicas da página do artista, ela é encontrada.
        val ranked = rankArtistTracks(local, "Dxrkaii", tracks)
        assertEquals(1L, ranked.first().trackId)
        assertTrue(ranked.first().confidence >= AUTO_PICK_CONFIDENCE)
    }
}
