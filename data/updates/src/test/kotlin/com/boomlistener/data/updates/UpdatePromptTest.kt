// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.updates

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatePromptTest {
    private val day = 24 * 60 * 60 * 1000L
    private val now = 100 * day

    @Test
    fun semNadaGuardadoAvisa() {
        assertTrue(shouldPrompt("0.22.0", now, UpdatePromptState()))
        assertTrue(shouldAutoCheck(now, UpdatePromptState()))
    }

    @Test
    fun adiadoNaoAvisaNemConsultaAteOPrazo() {
        val snoozed = UpdatePromptState(snoozedUntilMs = now + 7 * day)
        assertFalse(shouldPrompt("0.22.0", now, snoozed))
        assertFalse(shouldAutoCheck(now, snoozed))
        assertTrue(shouldPrompt("0.22.0", now + 8 * day, snoozed))
    }

    @Test
    fun versaoPuladaSoVoltaNaProxima() {
        val skipped = UpdatePromptState(skippedVersion = "0.22.0")
        assertFalse(shouldPrompt("0.22.0", now, skipped))
        assertFalse(shouldPrompt("v0.22.0", now, skipped))
        assertTrue(shouldPrompt("0.22.1", now, skipped))
    }

    @Test
    fun consultaAutomaticaNoMaximoUmaVezPorIntervalo() {
        // Consultou há meio intervalo: ainda não consulta de novo.
        val checked = UpdatePromptState(lastAutoCheckMs = now - AUTO_CHECK_INTERVAL_MS / 2)
        assertFalse(shouldAutoCheck(now, checked))
        assertTrue(shouldAutoCheck(now + AUTO_CHECK_INTERVAL_MS, checked))
    }
}
