// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.animation.EnterExitState
import org.junit.Assert.assertEquals
import org.junit.Test

class RecedeBlurTest {
    private fun target(
        state: EnterExitState,
        enabled: Boolean = true,
        settled: Boolean = false,
        goingBack: Boolean = false,
        tabSwitch: Boolean = false,
    ) = recedeTarget(enabled, settled, state, goingBack, tabSwitch)

    @Test
    fun abrindoATelaQueSaiFicaPorBaixoEDesfoca() {
        assertEquals(1f, target(EnterExitState.PostExit, goingBack = false))
        assertEquals(0f, target(EnterExitState.PreEnter, goingBack = false))
    }

    @Test
    fun voltandoATelaQueEntraEstavaPorBaixoEDesfoca() {
        assertEquals(1f, target(EnterExitState.PreEnter, goingBack = true))
        assertEquals(0f, target(EnterExitState.PostExit, goingBack = true))
    }

    @Test
    fun telaVisivelNuncaDesfoca() {
        assertEquals(0f, target(EnterExitState.Visible, goingBack = true))
        assertEquals(0f, target(EnterExitState.Visible, goingBack = false))
    }

    @Test
    fun telaDeCimaParadaNuncaFicaDesfocadaMesmoComEstadoTorto() {
        // Voltar preditivo começado e desistido, e o desfoque ligado de novo: a tela continua nítida.
        for (state in EnterExitState.entries) for (back in listOf(true, false)) {
            assertEquals("$state back=$back", 0f, target(state, settled = true, goingBack = back))
        }
    }

    @Test
    fun desligadoOuTrocaDeAbaNaoDesfoca() {
        for (state in EnterExitState.entries) for (back in listOf(true, false)) {
            assertEquals(0f, target(state, enabled = false, goingBack = back))
            assertEquals(0f, target(state, tabSwitch = true, goingBack = back))
        }
    }
}
