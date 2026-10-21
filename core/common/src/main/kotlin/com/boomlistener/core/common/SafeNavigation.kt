// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.common

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder

/**
 * A tela atual já terminou de abrir? Enquanto uma tela ainda está entrando
 * (ou saindo), ela não está RESUMED: um segundo toque rápido nesse meio-tempo
 * abriria a mesma tela de novo, uma por cima da outra.
 */
private fun NavController.isIdle(): Boolean =
    currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) ?: true

/**
 * Abre [route] uma vez só: toques repetidos enquanto a tela abre são
 * ignorados. Toda navegação do app passa por aqui (nunca `navigate` direto).
 */
fun NavController.navigateOnce(route: String, builder: NavOptionsBuilder.() -> Unit = {}) {
    if (!isIdle()) return
    navigate(route, builder)
}

/** "Voltar" de uma tela de dentro, uma vez só (dois toques não fecham duas telas). */
fun NavController.popBackStackOnce(): Boolean {
    if (!isIdle()) return false
    return popBackStack()
}
