// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.common

import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.NavOptionsBuilder

/**
 * Só o que é de fato um toque duplo é ignorado: dois pedidos de navegação com menos de
 * [DOUBLE_TAP_MS] entre si. Antes, qualquer toque enquanto a animação de entrada não terminava
 * (a tela ainda não estava RESUMED) era descartado, e quem tocava rápido em Ajustes e logo numa
 * opção via o segundo toque "não pegar".
 */
private const val DOUBLE_TAP_MS = 180L

private fun nowMs(): Long = System.nanoTime() / 1_000_000L

private val lastNavigation = java.util.WeakHashMap<NavController, Long>()

private fun NavController.isDoubleTap(): Boolean {
    val last = lastNavigation[this] ?: return false
    return nowMs() - last < DOUBLE_TAP_MS
}

/** A última tela aberta por [navigateOnce] em cada NavController (o app tem um só). */
private val lastOpened = java.util.WeakHashMap<NavController, String>()

/**
 * Abre [route] uma vez só: um toque duplo (menos de 180 ms) na tela que acabou de
 * abrir é ignorado (abriria a mesma tela por cima).
 * Só a tela que *entrou* bloqueia: depois de voltar, a tela de baixo aceita
 * o toque na hora, mesmo com a animação de saída ainda terminando.
 * Toda navegação do app passa por aqui (nunca `navigate` direto).
 */
fun NavController.navigateOnce(route: String, builder: NavOptionsBuilder.() -> Unit = {}) {
    val current = currentBackStackEntry
    if (current != null && current.id == lastOpened[this] && isDoubleTap()) return
    navigate(route, builder)
    lastNavigation[this] = nowMs()
    currentBackStackEntry?.let { lastOpened[this] = it.id }
}

/** TEMPORÁRIO (registro do "voltar"): a pilha por nome e estado (ex.: "grafo:RES>library:RES>settings:STA"). */
fun NavController.stackDescription(): String = currentBackStack.value.joinToString(">") {
    "${it.destination.route ?: "grafo"}:${it.lifecycle.currentState.name.take(3)}"
}

/** "Voltar" de uma tela de dentro, uma vez só (dois toques não fecham duas telas). */
fun NavController.popBackStackOnce(): Boolean {
    if (isDoubleTap()) {
        DiagnosticLog.log("voltar", "popBackStackOnce ignorado (toque duplo) pilha=${stackDescription()}")
        return false
    }
    // Só resta uma tela de verdade (a Home): voltar nela esvaziaria a pilha (tela vazia, e o app fechando
    // no voltar seguinte). Acontece quando a tela que acabou de sair, ainda na animação de saída, pede
    // voltar de novo (provado pelo registro de 25/11/2026). Quem sai do app é o sistema, não esta função.
    if (currentBackStack.value.count { it.destination !is NavGraph } <= 1) {
        DiagnosticLog.log("voltar", "popBackStackOnce recusado: só resta uma tela pilha=${stackDescription()}")
        return false
    }
    lastNavigation[this] = nowMs()
    DiagnosticLog.log("voltar", "popBackStackOnce pedido pilha=${stackDescription()}")
    val popped = popBackStack()
    DiagnosticLog.log("voltar", "popBackStackOnce resultado=$popped pilha=${stackDescription()}")
    return popped
}
