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

/** A última tela aberta por [navigateOnce] em cada NavController (o app tem um só). */
private val lastOpened = java.util.WeakHashMap<NavController, String>()

/**
 * Abre [route] uma vez só: toques repetidos enquanto a tela que acabou de
 * abrir ainda está entrando são ignorados (abririam a mesma tela por cima).
 * Só a tela que *entrou* bloqueia: depois de voltar, a tela de baixo aceita
 * o toque na hora, mesmo com a animação de saída ainda terminando.
 * Toda navegação do app passa por aqui (nunca `navigate` direto).
 */
fun NavController.navigateOnce(route: String, builder: NavOptionsBuilder.() -> Unit = {}) {
    val current = currentBackStackEntry
    if (current != null && current.id == lastOpened[this] && !isIdle()) return
    navigate(route, builder)
    currentBackStackEntry?.let { lastOpened[this] = it.id }
}

/** "Voltar" de uma tela de dentro, uma vez só (dois toques não fecham duas telas). */
fun NavController.popBackStackOnce(): Boolean {
    if (!isIdle()) return false
    return popBackStack()
}
