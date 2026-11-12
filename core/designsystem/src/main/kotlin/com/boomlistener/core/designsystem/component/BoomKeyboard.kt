// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.delay

/**
 * Tira o foco e fecha o teclado, e espera ele sair, antes de abrir uma janela do
 * sistema (pedido de apagar ou de gravar arquivo). Com o teclado ainda de pé atrás da
 * janela do sistema, os botões dela deixavam de sentir o toque.
 */
@Composable
fun rememberKeyboardDismisser(): suspend () -> Unit {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    return remember(focus, keyboard) {
        suspend {
            focus.clearFocus(force = true)
            keyboard?.hide()
            delay(KEYBOARD_EXIT_MS)
        }
    }
}

private const val KEYBOARD_EXIT_MS = 150L
