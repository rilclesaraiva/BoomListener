// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.boomlistener.core.designsystem.component.BoomSeekBar
import com.boomlistener.core.designsystem.component.BoomThinProgress

/**
 * A barra do player com a bolinha deslizando: o player informa o ponto da
 * música só a cada meio segundo, e a bolinha andava aos saltinhos. Entre um
 * aviso e outro, ela segue o relógio ([nextSmoothPositionMs]); o ponto real
 * continua mandando (toda correção é absorvida aos poucos, e um salto de
 * verdade, como pular na barra ou trocar de música, vai direto).
 *
 * Fica numa função à parte para só ela se redesenhar a cada quadro.
 */
@Composable
internal fun SmoothSeekBar(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    speed: Float,
    onSeek: (Float) -> Unit,
    dragLabel: (Float) -> String,
    modifier: Modifier = Modifier,
) {
    val shownMs = rememberSmoothPositionMs(positionMs, durationMs, isPlaying, speed)
    BoomSeekBar(
        progress = if (durationMs > 0) (shownMs / durationMs).coerceIn(0f, 1f) else 0f,
        isPlaying = isPlaying,
        onSeek = onSeek,
        dragLabel = dragLabel,
        modifier = modifier,
    )
}

/**
 * A barrinha fina do mini player, com o mesmo deslizar da barra do player
 * (a mesma conta de [rememberSmoothPositionMs]), em vez de andar aos saltos
 * a cada aviso do player.
 */
@Composable
internal fun SmoothThinProgress(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    speed: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val shownMs = rememberSmoothPositionMs(positionMs, durationMs, isPlaying, speed)
    BoomThinProgress(
        progress = if (durationMs > 0) (shownMs / durationMs).coerceIn(0f, 1f) else 0f,
        color = color,
        modifier = modifier,
    )
}

/** O ponto mostrado da música, andando com o relógio entre um aviso e outro do player. */
@Composable
internal fun rememberSmoothPositionMs(positionMs: Long, durationMs: Long, isPlaying: Boolean, speed: Float): Float {
    var shownMs by remember { mutableFloatStateOf(positionMs.toFloat()) }
    LaunchedEffect(positionMs, durationMs, isPlaying, speed) {
        if (!isPlaying || durationMs <= 0) {
            shownMs = positionMs.toFloat()
            return@LaunchedEffect
        }
        val reportedAt = System.nanoTime()
        var last = reportedAt
        while (true) {
            withFrameNanos {
                val now = System.nanoTime()
                shownMs = nextSmoothPositionMs(
                    shownMs = shownMs,
                    reportedMs = positionMs,
                    sinceReportMs = (now - reportedAt) / 1_000_000f,
                    frameMs = (now - last) / 1_000_000f,
                    speed = speed,
                    durationMs = durationMs,
                )
                last = now
            }
        }
    }
    return shownMs
}

/** Mais que isto de diferença é salto de verdade (pulou na barra, trocou de música): vai direto. */
private const val JUMP_MS = 1_500f

/** Sem aviso novo do player por mais que isto, a bolinha espera (não passa do ponto). */
private const val MAX_AHEAD_MS = 1_000f

/**
 * O próximo ponto da bolinha (puro e testado): o ponto informado mais o tempo
 * que passou desde então, alcançado aos poucos quando há uma pequena diferença.
 */
internal fun nextSmoothPositionMs(
    shownMs: Float,
    reportedMs: Long,
    sinceReportMs: Float,
    frameMs: Float,
    speed: Float,
    durationMs: Long,
): Float {
    val target = (reportedMs + minOf(sinceReportMs, MAX_AHEAD_MS) * speed).coerceIn(0f, durationMs.toFloat())
    if (kotlin.math.abs(target - shownMs) > JUMP_MS) return target
    // Anda junto com o relógio e fecha a diferença que sobrar em ~150 ms, sem tranco.
    val advanced = shownMs + frameMs * speed
    val catchUp = (frameMs / 150f).coerceIn(0f, 1f)
    return (advanced + (target - advanced) * catchUp).coerceIn(0f, durationMs.toFloat())
}
