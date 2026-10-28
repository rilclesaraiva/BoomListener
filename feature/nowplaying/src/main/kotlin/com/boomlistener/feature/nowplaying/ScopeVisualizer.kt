// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import com.boomlistener.core.player.AudioVisualizer
import com.boomlistener.core.player.BEAM_LEVELS
import com.boomlistener.core.player.beamIntensity
import com.boomlistener.core.player.beamLevel
import kotlin.math.exp
import kotlin.math.hypot

// Amostras guardadas por quadro (sobra para 192 kHz a 60 quadros por segundo).
private const val SCOPE_MAX_FRAMES = 4096

// Na primeira leitura (ou depois de um pulo), quantas amostras para trás.
private const val SCOPE_FIRST_READ = 1024

// Rastro do fósforo: quantos quadros ficam guardados e quanto tempo duram.
private const val SCOPE_TRAILS = 8
private const val SCOPE_PERSIST_NANOS = 120_000_000L
private const val SCOPE_DECAY_NANOS = 35_000_000.0

/** Um quadro do feixe: as amostras (esquerdo, direito) e quando chegaram. */
private class ScopeTrail {
    val left = FloatArray(SCOPE_MAX_FRAMES)
    val right = FloatArray(SCOPE_MAX_FRAMES)
    var count = 0
    var atNanos = Long.MIN_VALUE
}

/**
 * Osciloscópio XY, como o da "oscilloscope music" (Jerobeam Fenderson): o
 * canal esquerdo move o feixe na horizontal e o direito na vertical, amostra
 * por amostra. Música feita para isso desenha imagens; música comum desenha
 * as formas do próprio estéreo. O feixe brilha mais onde passa devagar
 * ([beamIntensity]), as cores se somam onde ele cruza e o rastro apaga em
 * pouco mais de 0,1 s, como o fósforo de uma tela antiga.
 *
 * O fundo escuro fica por conta de quem chama ([CoverVisualizer]).
 */
@Composable
internal fun ScopeVisualizer(playing: Boolean, modifier: Modifier = Modifier) {
    val beam = MaterialTheme.colorScheme.primary.toArgb()
    val trails = remember { Array(SCOPE_TRAILS) { ScopeTrail() } }
    val cursor = remember { LongArray(2) { -1L } } // [0] = última amostra lida, [1] = próximo rastro
    var frame by remember { mutableLongStateOf(0L) }

    // Só guarda o som estéreo enquanto o osciloscópio está na tela (com o app aberto).
    LifecycleStartEffect(Unit) {
        AudioVisualizer.attachScope()
        onStopOrDispose { AudioVisualizer.detachScope() }
    }
    LaunchedEffect(playing) {
        var settled = false
        // Pausado, o rastro apaga e para de redesenhar (sem gastar bateria).
        while (!settled) {
            withFrameNanos { now ->
                val end = if (playing) AudioVisualizer.scopeFrameNow() else -1L
                if (end >= 0) {
                    val last = cursor[0]
                    // Começa uma amostra antes do fim do quadro anterior, para o feixe não ter emendas.
                    val from = if (last < 0 || end < last || end - last > SCOPE_MAX_FRAMES) end - SCOPE_FIRST_READ else last - 1
                    val trail = trails[(cursor[1] % SCOPE_TRAILS).toInt()]
                    val count = AudioVisualizer.scopeCopy(from, end, trail.left, trail.right)
                    if (count > 1) {
                        trail.count = count
                        trail.atNanos = now
                        cursor[1]++
                    }
                    cursor[0] = end
                } else if (!playing) {
                    cursor[0] = -1
                }
                frame = now
                settled = !playing && trails.none { it.atNanos != Long.MIN_VALUE && now - it.atNanos < SCOPE_PERSIST_NANOS }
            }
        }
    }

    val glow = remember { scopePaint() }
    val core = remember { scopePaint() }
    val lines = remember { Array(BEAM_LEVELS.size) { FloatArray(SCOPE_MAX_FRAMES * 4) } }
    val counts = remember { IntArray(BEAM_LEVELS.size) }

    // Camada própria: o feixe soma luz só por cima do fundo escuro, não da tela inteira.
    Canvas(modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        val now = frame
        val side = minOf(size.width, size.height) * 0.9f
        val half = side / 2
        val centerX = size.width / 2
        val centerY = size.height / 2
        // Um trecho deste tamanho (ou menor) acende por inteiro.
        val focus = side * 0.025f
        glow.strokeWidth = 7.dp.toPx()
        core.strokeWidth = 1.8.dp.toPx()
        var drewAny = false
        for (trail in trails) {
            val age = now - trail.atNanos
            if (trail.atNanos == Long.MIN_VALUE || age >= SCOPE_PERSIST_NANOS || trail.count < 2) continue
            val fade = exp(-age / SCOPE_DECAY_NANOS).toFloat()
            counts.fill(0)
            var x0 = centerX + trail.left[0] * half
            var y0 = centerY - trail.right[0] * half
            for (k in 1 until trail.count) {
                val x1 = centerX + trail.left[k] * half
                val y1 = centerY - trail.right[k] * half
                val level = beamLevel(beamIntensity(hypot(x1 - x0, y1 - y0), focus))
                if (level >= 0) {
                    val out = lines[level]
                    val at = counts[level]
                    out[at] = x0
                    out[at + 1] = y0
                    out[at + 2] = x1
                    out[at + 3] = y1
                    counts[level] = at + 4
                }
                x0 = x1
                y0 = y1
            }
            for (level in BEAM_LEVELS.indices) {
                if (counts[level] == 0) continue
                val strength = BEAM_LEVELS[level] * fade
                drawBeam(lines[level], counts[level], glow, beam, strength * 0.16f)
                drawBeam(lines[level], counts[level], core, beam, strength)
                drewAny = true
            }
        }
        // Sem som: o ponto parado no meio, como num osciloscópio ligado.
        if (!drewAny) {
            val dot = floatArrayOf(centerX, centerY, centerX, centerY)
            drawBeam(dot, 4, glow, beam, 0.2f)
            drawBeam(dot, 4, core, beam, 0.7f)
        }
    }
}

private fun scopePaint() = Paint().apply {
    isAntiAlias = true
    style = Paint.Style.STROKE
    strokeCap = Paint.Cap.ROUND
    // Somar luz: onde o feixe cruza com ele mesmo, fica mais claro.
    xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
}

private fun DrawScope.drawBeam(points: FloatArray, count: Int, paint: Paint, color: Int, alpha: Float) {
    paint.color = color
    paint.alpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
    drawContext.canvas.nativeCanvas.drawLines(points, 0, count, paint)
}
