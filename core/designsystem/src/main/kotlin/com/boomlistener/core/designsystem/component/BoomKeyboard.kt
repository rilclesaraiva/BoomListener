// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.boomlistener.core.common.DiagnosticLog
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
    val view = LocalView.current
    return remember(focus, keyboard, view) {
        suspend {
            DiagnosticLog.log("teclado", "fechando antes da janela do sistema (teclado=${imeVisible(view)})")
            focus.clearFocus(force = true)
            keyboard?.hide()
            delay(KEYBOARD_EXIT_MS)
            DiagnosticLog.log("teclado", "depois da espera (teclado=${imeVisible(view)})")
        }
    }
}

private const val KEYBOARD_EXIT_MS = 150L

/** O teclado está na tela agora? (para o registro de diagnóstico) */
fun imeVisible(view: android.view.View): Boolean =
    ViewCompat.getRootWindowInsets(view)?.isVisible(WindowInsetsCompat.Type.ime()) == true
