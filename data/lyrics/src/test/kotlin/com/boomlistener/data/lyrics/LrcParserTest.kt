// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
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

    @Test
    fun reconheceLetraComTempos() {
        assertEquals(true, hasSyncedLyrics("[00:01.00] Açaí\n[00:04.00] guardiã"))
        assertEquals(false, hasSyncedLyrics("Primeira\nSegunda"))
        assertEquals(false, hasSyncedLyrics("  "))
        assertEquals(false, hasSyncedLyrics(null))
    }

    @Test
    fun letraSincronizadaViraLrcEVoltaIgual() {
        val lyrics = Lyrics(
            lines = listOf(LyricLine(1_000, "Açaí"), LyricLine(65_430, ""), LyricLine(125_990, "guardiã [feat.]")),
            synced = true,
            source = "LRCLIB",
        )
        val text = lyrics.toLrcText()
        assertEquals("[00:01.00]Açaí\n[01:05.43]\n[02:05.99]guardiã [feat.]", text)
        assertEquals(lyrics.lines, parseLrc(text))
    }

    @Test
    fun letraSemTemposViraSoAsLinhas() {
        val lyrics = Lyrics(listOf(LyricLine(0, "Primeira"), LyricLine(0, "Segunda")), synced = false, source = "LRCLIB")
        assertEquals("Primeira\nSegunda", lyrics.toLrcText())
    }

    @Test
    fun comparaALetraComOTextoDaTag() {
        val lyrics = Lyrics(listOf(LyricLine(1_000, "Açaí"), LyricLine(65_400, "guardiã")), synced = true, source = "LRCLIB")
        assertEquals(true, lyrics.matchesText("[00:01.00]Açaí\n[01:05.40]guardiã"))
        assertEquals(true, lyrics.matchesText("[00:01.000] Açaí\r\n[01:05.4]guardiã\n"))
        assertEquals(false, lyrics.matchesText("[00:01.50]Açaí\n[01:05.40]guardiã")) // tempo diferente
        assertEquals(false, lyrics.matchesText("[00:01.00]Açaí\n[01:05.40]guardião")) // texto diferente
        assertEquals(false, lyrics.matchesText("Açaí\nguardiã")) // sem tempos
        assertEquals(false, lyrics.matchesText(null))
    }
}
