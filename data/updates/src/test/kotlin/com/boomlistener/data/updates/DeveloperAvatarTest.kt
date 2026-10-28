// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.updates

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeveloperAvatarTest {
    private val now = 1_800_000_000_000L

    @Test
    fun nuncaPerguntouPergunta() {
        assertTrue(shouldRefreshAvatar(now, lastAttemptMs = 0L, hasFile = false))
        assertTrue(shouldRefreshAvatar(now, lastAttemptMs = 0L, hasFile = true))
    }

    @Test
    fun comAFotoGuardadaSoUmaVezPorSemana() {
        assertFalse(shouldRefreshAvatar(now, now - AVATAR_REFRESH_MS + 1, hasFile = true))
        assertTrue(shouldRefreshAvatar(now, now - AVATAR_REFRESH_MS, hasFile = true))
    }

    @Test
    fun semAFotoTentaDeNovoDepoisDeUmTempo() {
        assertFalse(shouldRefreshAvatar(now, now - 60_000, hasFile = false))
        assertTrue(shouldRefreshAvatar(now, now - AVATAR_RETRY_MS, hasFile = false))
    }

    @Test
    fun relogioQueVoltouConfereDeNovo() {
        assertTrue(shouldRefreshAvatar(now, now + 60_000, hasFile = true))
    }
}
