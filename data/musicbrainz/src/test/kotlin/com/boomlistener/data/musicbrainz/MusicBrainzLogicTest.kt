// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.musicbrainz

import com.boomlistener.core.model.ReleaseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicBrainzLogicTest {

    private fun option(
        id: String,
        date: String = "2000-01-01",
        official: Boolean = true,
        primary: String? = "Album",
        secondary: List<String> = emptyList(),
    ) = MbReleaseOption(id, "g-$id", "T", date, official, primary, secondary, "1", 10, 1, 1)

    @Test
    fun chaveSempreNegativaEEstavel() {
        val id = "f27ec8db-af05-4f36-916e-3d57f91ecf5e"
        assertTrue(musicBrainzKey(id) < 0)
        assertEquals(musicBrainzKey(id), musicBrainzKey(id))
        assertNotEquals(musicBrainzKey(id), musicBrainzKey("a74b1b7f-71a5-4011-9441-d0b5e4122711"))
        assertTrue(musicBrainzKey("não é uuid") < 0)
    }

    @Test
    fun aspasEscapadasNaBusca() {
        assertEquals("\"Say \\\"Hi\\\"\"", luceneQuote("Say \"Hi\""))
        assertEquals("\"AC\\\\DC\"", luceneQuote("AC\\DC"))
    }

    @Test
    fun tipoDoLancamento() {
        assertEquals(ReleaseType.Album, releaseTypeOf("Album", emptyList()))
        assertEquals(ReleaseType.EP, releaseTypeOf("EP", emptyList()))
        assertEquals(ReleaseType.Single, releaseTypeOf("Single", emptyList()))
        assertEquals(ReleaseType.Compilation, releaseTypeOf("Album", listOf("Compilation")))
        assertNull(releaseTypeOf("Album", listOf("Live")))
        assertNull(releaseTypeOf("Album", listOf("Soundtrack")))
        assertNull(releaseTypeOf("Broadcast", emptyList()))
        assertNull(releaseTypeOf(null, emptyList()))
    }

    @Test
    fun lancamentoOriginalGanha() {
        val best = bestRelease(
            listOf(
                option("coletanea", date = "1990", secondary = listOf("Compilation")),
                option("bootleg", date = "1985", official = false),
                option("single", date = "1986", primary = "Single"),
                option("reedicao", date = "2010"),
                option("original", date = "1987-05-01"),
            ),
        )
        assertEquals("original", best?.id)
        assertNull(bestRelease(emptyList()))
    }

    @Test
    fun semDataVaiParaOFim() {
        assertEquals("datado", bestRelease(listOf(option("semdata", date = ""), option("datado", date = "2001")))?.id)
    }

    @Test
    fun artistaComAcentoEFeat() {
        val hits = listOf(
            MbArtistHit("1", "Joao Gilberto", 80, emptyList()),
            MbArtistHit("2", "João Gilberto", 100, emptyList()),
            MbArtistHit("3", "João Gilberto Trio", 90, emptyList()),
        )
        val found = matchingArtists("JOÃO GILBERTO feat. Stan Getz", hits)
        assertEquals(listOf("2", "1"), found.map { it.id })
    }

    @Test
    fun artistaPeloApelido() {
        val hits = listOf(MbArtistHit("1", "Beyoncé Knowles", 100, listOf("Beyonce", "Beyoncé")))
        assertEquals("1", matchingArtists("Beyoncé", hits).single().id)
        assertTrue(matchingArtists("", hits).isEmpty())
    }

    @Test
    fun homonimosPelosAlbuns() {
        val local = listOf("Nevermind", "In Utero (Remastered)", "nevermind")
        assertEquals(2, albumsInCommon(local, listOf("Nevermind", "In Utero", "Bleach")))
        assertEquals(0, albumsInCommon(local, listOf("Outro Disco")))
    }

    @Test
    fun sugestaoDeTags() {
        val recording = MbRecording(
            recordingId = "r", releaseId = "rel", releaseGroupId = "g",
            title = "Águas de Março", artist = "Elis Regina & Tom Jobim", album = "Elis & Tom",
            durationMs = 212_000, date = "1974-03-01", trackNumber = "1", trackTotal = "14",
            discNumber = "", discTotal = "",
        )
        val suggestion = recording.toTagSuggestion(0.9f)
        assertEquals("1974", suggestion.tags.year)
        assertEquals("Elis & Tom", suggestion.tags.album)
        assertEquals("MusicBrainz", suggestion.source)
        assertEquals("https://coverartarchive.org/release/rel/front", suggestion.coverUrl)
        assertEquals("", recording.copy(date = "").toTagSuggestion(0.9f).tags.year)
    }
}
