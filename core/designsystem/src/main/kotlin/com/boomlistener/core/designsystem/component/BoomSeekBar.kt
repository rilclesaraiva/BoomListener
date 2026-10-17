// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap

/**
 * Barra de progresso da música, no estilo do player do Android 16: a trilha
 * fica ondulada enquanto toca e reta quando pausa. Dá para arrastar.
 *
 * @param progress de 0 a 1.
 * @param onSeek chamado ao soltar o dedo, com a nova posição de 0 a 1.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BoomSeekBar(
    progress: Float,
    isPlaying: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Enquanto o dedo está na barra, ela segue o dedo e não o player.
    var dragging by remember { mutableStateOf<Float?>(null) }
    val shown = dragging ?: progress

    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (isPlaying && dragging == null) {
            LinearWavyProgressIndicator(
                progress = { shown },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        } else {
            LinearProgressIndicator(
                progress = { shown },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeCap = StrokeCap.Round,
            )
        }
        // Slider invisível por cima: só cuida do toque e do arraste.
        Slider(
            value = shown,
            onValueChange = { dragging = it },
            onValueChangeFinished = {
                dragging?.let(onSeek)
                dragging = null
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Linha fina de progresso, para o mini player. */
@Composable
fun BoomThinProgress(progress: Float, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        trackColor = Color.Transparent,
        drawStopIndicator = {},
    )
}
