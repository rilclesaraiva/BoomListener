// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.player

import org.junit.Assert.assertEquals
import org.junit.Test

class KeepScreenOnTest {
    private fun keep(
        vararg places: ScreenOnPlace,
        onlyPlaying: Boolean = false,
        playing: Boolean = true,
        tab: ScreenOnPlace? = ScreenOnPlace.Home,
        playerOpen: Boolean = false,
        lyrics: Boolean = false,
    ) = shouldKeepScreenOn(places.toSet(), onlyPlaying, playing, tab, playerOpen, lyrics)

    @Test
    fun semNadaMarcadoNuncaFicaAcesa() {
        assertEquals(false, keep(playerOpen = true, lyrics = true))
    }

    @Test
    fun playerCobreCapaELetra() {
        assertEquals(true, keep(ScreenOnPlace.Player, playerOpen = true))
        assertEquals(true, keep(ScreenOnPlace.Player, playerOpen = true, lyrics = true))
        assertEquals(false, keep(ScreenOnPlace.Player))
    }

    @Test
    fun soLetraSoComALetraNaTela() {
        assertEquals(false, keep(ScreenOnPlace.Lyrics, playerOpen = true))
        assertEquals(true, keep(ScreenOnPlace.Lyrics, playerOpen = true, lyrics = true))
        // Letra "ligada" mas o player fechado: a letra não está na tela.
        assertEquals(false, keep(ScreenOnPlace.Lyrics, lyrics = true))
    }

    @Test
    fun abaSoContaComOPlayerFechado() {
        assertEquals(true, keep(ScreenOnPlace.Home))
        assertEquals(false, keep(ScreenOnPlace.Home, tab = ScreenOnPlace.Library))
        assertEquals(false, keep(ScreenOnPlace.Home, playerOpen = true))
        assertEquals(false, keep(ScreenOnPlace.Home, tab = null))
    }

    @Test
    fun appTodoValeEmQualquerTela() {
        assertEquals(true, keep(ScreenOnPlace.Everywhere, tab = null))
        assertEquals(true, keep(ScreenOnPlace.Everywhere, playerOpen = true))
    }

    @Test
    fun soTocandoApagaQuandoPausa() {
        assertEquals(true, keep(ScreenOnPlace.Everywhere, onlyPlaying = true, playing = true))
        assertEquals(false, keep(ScreenOnPlace.Everywhere, onlyPlaying = true, playing = false))
        assertEquals(true, keep(ScreenOnPlace.Everywhere, onlyPlaying = false, playing = false))
    }
}
