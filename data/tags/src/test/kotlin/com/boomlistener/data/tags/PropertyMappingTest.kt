// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.tags

import com.boomlistener.core.model.TrackTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PropertyMappingTest {

    private val mp3 = mapOf(
        "TITLE" to listOf("Açaí"),
        "ARTIST" to listOf("Djavan"),
        "ALBUM" to listOf("Luz"),
        "DATE" to listOf("1982-05-01"),
        "TRACKNUMBER" to listOf("03/10"),
        "GENRE" to listOf("MPB"),
        "COMMENT" to listOf("ripado do vinil"),
        "ISRC" to listOf("BRXXX8200001"),
    )

    @Test
    fun leTagsComAcentoEFaixaComTotal() {
        val tags = tagsFromProperties(mp3)
        assertEquals("Açaí", tags.title)
        assertEquals("Djavan", tags.artist)
        assertEquals("1982", tags.year)
        assertEquals("3", tags.trackNumber)
        assertEquals("10", tags.trackTotal)
    }

    @Test
    fun chavesEmMinusculaTambemValem() {
        val tags = tagsFromProperties(mapOf("title" to listOf("Oceano"), "artist" to listOf("Djavan")))
        assertEquals("Oceano", tags.title)
        assertEquals("Djavan", tags.artist)
    }

    @Test
    fun variosArtistasViramUmCampo() {
        val tags = tagsFromProperties(mapOf("ARTIST" to listOf("Anitta", "J Balvin")))
        assertEquals("Anitta; J Balvin", tags.artist)
    }

    @Test
    fun gravarMantemOQueOEditorNaoMostra() {
        val edited = tagsFromProperties(mp3).copy(title = "Açaí (Ao Vivo)")
        val result = applyTags(mp3, edited)
        assertEquals(listOf("Açaí (Ao Vivo)"), result["TITLE"])
        assertEquals(listOf("ripado do vinil"), result["COMMENT"])
        assertEquals(listOf("BRXXX8200001"), result["ISRC"])
    }

    @Test
    fun anoIgualMantemDataCompleta() {
        val result = applyTags(mp3, tagsFromProperties(mp3))
        assertEquals(listOf("1982-05-01"), result["DATE"])
    }

    @Test
    fun anoNovoTrocaAData() {
        val result = applyTags(mp3, tagsFromProperties(mp3).copy(year = "1983"))
        assertEquals(listOf("1983"), result["DATE"])
    }

    @Test
    fun campoVazioApagaATag() {
        val result = applyTags(mp3, tagsFromProperties(mp3).copy(genre = "", album = "  "))
        assertFalse("GENRE" in result)
        assertFalse("ALBUM" in result)
    }

    @Test
    fun faixaComTotalGravaJunto() {
        val result = applyTags(emptyMap(), TrackTags(title = "x", trackNumber = "07", trackTotal = "12"))
        assertEquals(listOf("7/12"), result["TRACKNUMBER"])
    }

    @Test
    fun flacComTotalSeparadoContinuaSeparado() {
        val flac = mapOf("TRACKNUMBER" to listOf("2"), "TRACKTOTAL" to listOf("9"))
        val result = applyTags(flac, tagsFromProperties(flac).copy(trackNumber = "4"))
        assertEquals(listOf("4"), result["TRACKNUMBER"])
        assertEquals(listOf("9"), result["TRACKTOTAL"])
    }

    @Test
    fun variosArtistasSemMudancaMantemALista() {
        val original = mapOf("ARTIST" to listOf("Anitta", "J Balvin"))
        val result = applyTags(original, tagsFromProperties(original))
        assertEquals(listOf("Anitta", "J Balvin"), result["ARTIST"])
    }

    @Test
    fun letraMantemQuebrasDeLinha() {
        val lyrics = "[00:01.00] Primeira\n[00:05.00] Segunda"
        val result = applyTags(mapOf("UNSYNCEDLYRICS" to listOf("velha")), TrackTags(title = "x", lyrics = "\n$lyrics\n"))
        assertEquals(listOf(lyrics), result["LYRICS"])
        assertFalse("UNSYNCEDLYRICS" in result)
    }

    @Test
    fun numeros() {
        assertEquals("3" to "12", splitNumber("03/12"))
        assertEquals("" to "", splitNumber(""))
        assertEquals("", cleanNumber("0"))
        assertEquals("2023", yearOf("2023-01-01T00:00"))
    }
}
