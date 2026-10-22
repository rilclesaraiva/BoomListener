// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Painel que sobe de baixo, usado no app inteiro no lugar do
 * `ModalBottomSheet` direto.
 *
 * A diferença é o "voltar": o do M3 encolhe o painel com a animação do
 * sistema enquanto o gesto acontece. Aqui o voltar é nosso: o painel fecha
 * descendo, com a mesma animação de fechar de sempre.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoomModalSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    sheetGesturesEnabled: Boolean = true,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    /** Falso enquanto o painel não pode fechar (ex.: salvando). */
    dismissEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val dismiss by rememberUpdatedState(onDismissRequest)
    ModalBottomSheet(
        onDismissRequest = { if (dismissEnabled) dismiss() },
        modifier = modifier,
        sheetState = sheetState,
        sheetGesturesEnabled = sheetGesturesEnabled,
        containerColor = containerColor,
        contentColor = contentColor,
        // Sem o voltar do M3 (e a animação de encolher do sistema).
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
    ) {
        PredictiveBackHandler(enabled = dismissEnabled) { progress ->
            try {
                progress.collect { }
                scope.launch { sheetState.hide() }.invokeOnCompletion { dismiss() }
            } catch (cancelled: CancellationException) {
                // Gesto desistido: o painel continua aberto.
                throw cancelled
            }
        }
        content()
    }
}
