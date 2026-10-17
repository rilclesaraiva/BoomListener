// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.lyrics

import com.boomlistener.core.model.LyricLine
import com.boomlistener.core.model.Lyrics
import org.junit.Assert.assertEquals
import org.junit.Test

class LrcParserTest {
    @Test
    fun leTempoETexto() {
        val lines = parseLrc("[00:12.34] Primeira linha\n[01:02.5]Segunda")
        assertEquals(listOf(LyricLine(12_340, "Primeira linha"), LyricLine(62_500, "Segunda")), lines)
    }

    @Test
    fun variasMarcasNaMesmaLinhaViramRefraoRepetido() {
        val lines = parseLrc("[00:10.00][00:30.00] Refrão\n[00:20.00] Verso")
        assertEquals(listOf(10_000L, 20_000L, 30_000L), lines.map { it.timeMs })
        assertEquals("Refrão", lines[2].text)
    }

    @Test
    fun ignoraTagsDeInformacaoEAplicaOffset() {
        val lines = parseLrc("[ar:Djavan]\n[ti:Açaí]\n[offset:500]\n[00:10.000] Açaí, guardiã")
        assertEquals(listOf(LyricLine(9_500, "Açaí, guardiã")), lines)
    }

    @Test
    fun linhaVaziaComTempoViraPausa() {
        val lines = parseLrc("[00:05.00] Oi\n[00:08.00]\n[00:12.00] Tchau")
        assertEquals("", lines[1].text)
    }

    @Test
    fun linhaAtualPorPosicao() {
        val lines = parseLrc("[00:10.00] a\n[00:20.00] b\n[00:30.00] c")
        assertEquals(-1, currentLyricIndex(lines, 5_000))
        assertEquals(0, currentLyricIndex(lines, 10_000))
        assertEquals(1, currentLyricIndex(lines, 29_999))
        assertEquals(2, currentLyricIndex(lines, 90_000))
    }

    @Test
    fun cacheIdaEVolta() {
        val original = Lyrics(parseLrc("[00:01.25] um\n[02:03.400] dois"), synced = true, source = "LRCLIB")
        assertEquals(original.lines, parseLrc(toCacheText(original)))
    }

    @Test
    fun letraSemTempo() {
        assertEquals(listOf("um", "", "dois"), parsePlain("\num\n\ndois\n").map { it.text })
    }

    @Test
    fun letraDoArquivoSincronizada() {
        val lyrics = lyricsFromText("[00:01.00] Açaí\n[00:04.00] guardiã")!!
        assertEquals(true, lyrics.synced)
        assertEquals(EMBEDDED_LYRICS_SOURCE, lyrics.source)
        assertEquals(2, lyrics.lines.size)
    }

    @Test
    fun letraDoArquivoSimples() {
        val lyrics = lyricsFromText("Primeira\nSegunda")!!
        assertEquals(false, lyrics.synced)
        assertEquals(listOf("Primeira", "Segunda"), lyrics.lines.map { it.text })
    }

    @Test
    fun letraDoArquivoVazia() {
        assertEquals(null, lyricsFromText("  \n "))
        assertEquals(null, lyricsFromText(null))
    }
}
