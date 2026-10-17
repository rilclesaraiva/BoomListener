// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackStateTest {
    @Test
    fun repeticaoAlternaNaOrdem() {
        assertEquals(RepeatMode.All, RepeatMode.Off.next())
        assertEquals(RepeatMode.One, RepeatMode.All.next())
        assertEquals(RepeatMode.Off, RepeatMode.One.next())
    }

    @Test
    fun progressoFicaEntreZeroEUm() {
        assertEquals(0.5f, PlaybackState(positionMs = 50, durationMs = 100).progress, 0.001f)
        assertEquals(0f, PlaybackState(positionMs = 50, durationMs = 0).progress, 0.001f)
        assertEquals(1f, PlaybackState(positionMs = 150, durationMs = 100).progress, 0.001f)
    }
}
