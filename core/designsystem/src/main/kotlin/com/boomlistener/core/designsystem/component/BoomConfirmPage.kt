// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Página de confirmação dentro do painel do menu ([BoomMenuAction.page]), no lugar de uma
 * janela de diálogo: título, explicação, o botão que confirma ([confirmLabel], em vermelho
 * quando [destructive]) e "Cancelar", que fecha o painel. Confirmar chama [onConfirm] e fecha.
 */
@Composable
fun BoomMenuPageScope.BoomConfirmPage(
    title: String,
    message: String,
    confirmLabel: String,
    cancelLabel: String,
    onConfirm: () -> Unit,
    destructive: Boolean = true,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(
            onClick = {
                onConfirm()
                dismiss()
            },
            colors = if (destructive) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            } else {
                ButtonDefaults.buttonColors()
            },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) { Text(confirmLabel) }
        TextButton(onClick = { dismiss() }, modifier = Modifier.fillMaxWidth()) { Text(cancelLabel) }
    }
}
