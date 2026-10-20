// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import android.text.format.DateFormat
import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.boomlistener.core.designsystem.R
import java.util.Date

/** O que a janela "Detalhes" mostra de uma música; null = o aparelho não informa. */
data class BoomSongDetails(
    val path: String?,
    val fileName: String?,
    val sizeBytes: Long?,
    val modifiedMs: Long?,
    val format: String?,
    val durationMs: Long?,
    val bitrateKbps: Int?,
    val sampleRateHz: Int?,
)

/**
 * Janela "Detalhes" de uma música: onde está, tamanho, formato, taxa de bits…
 * [details] null = ainda carregando. O texto pode ser selecionado (para copiar o caminho).
 */
@Composable
fun BoomSongDetailsDialog(title: String, details: BoomSongDetails?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, maxLines = 2) },
        text = {
            if (details == null) {
                CircularProgressIndicator()
                return@AlertDialog
            }
            val rows = listOfNotNull(
                details.path?.let { stringResource(R.string.core_designsystem_details_path) to it },
                details.fileName?.takeIf { details.path == null }?.let { stringResource(R.string.core_designsystem_details_file) to it },
                details.format?.let { stringResource(R.string.core_designsystem_details_format) to it },
                details.sizeBytes?.let { stringResource(R.string.core_designsystem_details_size) to Formatter.formatFileSize(context, it) },
                details.durationMs?.let { stringResource(R.string.core_designsystem_details_duration) to formatDetailsDuration(it) },
                details.bitrateKbps?.let { stringResource(R.string.core_designsystem_details_bitrate) to stringResource(R.string.core_designsystem_details_kbps, it) },
                details.sampleRateHz?.let { stringResource(R.string.core_designsystem_details_sample_rate) to stringResource(R.string.core_designsystem_details_hz, it) },
                details.modifiedMs?.let {
                    stringResource(R.string.core_designsystem_details_modified) to
                        "${DateFormat.getMediumDateFormat(context).format(Date(it))} ${DateFormat.getTimeFormat(context).format(Date(it))}"
                },
            )
            SelectionContainer {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rows.forEach { (label, value) ->
                        Column {
                            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.core_designsystem_details_close)) }
        },
    )
}

/** 254000 → "4:14"; com hora, "1:02:03". */
internal fun formatDetailsDuration(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
