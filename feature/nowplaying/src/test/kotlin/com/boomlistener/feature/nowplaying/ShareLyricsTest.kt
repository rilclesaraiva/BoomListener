// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareLyricsTest {
    private val times = longArrayOf(10_000, 14_000, 18_000, 25_000)

    @Test
    fun antesDoPrimeiroVersoNenhumEmDestaque() {
        val frame = shareLyricsFrame(times, 5_000)
        assertEquals(-1, frame.current)
        assertEquals(-1, frame.previous)
    }

    @Test
    fun versoAtualEOAnterior() {
        val frame = shareLyricsFrame(times, 19_000)
        assertEquals(2, frame.current)
        assertEquals(1, frame.previous)
        assertEquals(1f, frame.progress)
    }

    @Test
    fun passagemEntreVersosDeZeroAUmSemTranco() {
        val start = shareLyricsFrame(times, 14_000)
        assertEquals(0f, start.progress)
        val middle = shareLyricsFrame(times, 14_000 + LYRIC_TRANSITION_MS / 2)
        assertEquals(0.5f, middle.progress, 0.001f)
        val quarter = shareLyricsFrame(times, 14_000 + LYRIC_TRANSITION_MS / 4)
        // Suavizado: começa mais devagar que uma reta.
        assertTrue(quarter.progress < 0.25f)
        assertEquals(1f, shareLyricsFrame(times, 14_000 + LYRIC_TRANSITION_MS).progress)
    }

    @Test
    fun imagemMostraOPrimeiroVersoDoTrecho() {
        assertEquals(14_000L, staticLyricsTime(times, ShareClip(12_000, 42_000)))
        // Trecho sem nenhum começo de verso: o começo do trecho.
        assertEquals(26_000L, staticLyricsTime(times, ShareClip(26_000, 56_000)))
    }

    @Test
    fun letraVazia() {
        assertEquals(-1, shareLyricsFrame(LongArray(0), 30_000).current)
    }
}
