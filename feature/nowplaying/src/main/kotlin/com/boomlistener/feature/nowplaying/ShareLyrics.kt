// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.graphics.ColorUtils
import com.boomlistener.core.model.LyricLine

/*
 * Banner com a letra: a letra sincronizada rola no lugar da capa, com a
 * linha que está sendo cantada em destaque e as outras esmaecidas, igual à
 * tela de letra do player. Serve à prévia do painel, à imagem (o primeiro
 * verso do trecho) e ao vídeo (redesenhada a cada quadro).
 */

/** Quanto dura a passagem de uma linha para a seguinte (rolagem e destaque). */
internal const val LYRIC_TRANSITION_MS = 450L

/**
 * O que mostrar num instante: a linha atual ([current], -1 antes da primeira),
 * a de antes ([previous]) e quanto da passagem entre elas já foi ([progress],
 * 0 a 1, já suavizado).
 */
internal data class LyricsFrame(val current: Int, val previous: Int, val progress: Float)

/** O quadro da letra no momento [positionMs] da música (tempos das linhas em [times], em ordem). */
internal fun shareLyricsFrame(times: LongArray, positionMs: Long): LyricsFrame {
    var current = -1
    for (i in times.indices) {
        if (times[i] <= positionMs) current = i else break
    }
    if (current < 0) return LyricsFrame(current = -1, previous = -1, progress = 1f)
    val linear = ((positionMs - times[current]).toFloat() / LYRIC_TRANSITION_MS).coerceIn(0f, 1f)
    // Começa e termina devagar (sem tranco).
    val eased = linear * linear * (3 - 2 * linear)
    return LyricsFrame(current = current, previous = current - 1, progress = eased)
}

/**
 * O momento da imagem parada: o primeiro verso que começa dentro do trecho
 * (para a imagem mostrar uma linha de verdade em destaque); se o trecho não
 * tem nenhum começo de verso, o próprio começo do trecho.
 */
internal fun staticLyricsTime(times: LongArray, clip: ShareClip): Long =
    times.firstOrNull { it >= clip.startMs && it < clip.endMs } ?: clip.startMs

/** A letra pronta para desenhar: uma linha de texto já medida por verso. */
internal class ShareLyricsLayout(lines: List<LyricLine>, private val width: Int) {
    val times: LongArray = LongArray(lines.size) { lines[it].timeMs }

    private val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = LYRIC_TEXT_SIZE
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }

    // Verso vazio (pausa instrumental) vira uma nota, para não sumir do ritmo.
    private val layouts: List<StaticLayout> = lines.map { line ->
        val text = line.text.trim().ifEmpty { "♪" }
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(MAX_LINES_PER_VERSE)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setIncludePad(false)
            .build()
    }

    // Onde começa cada verso, contando do topo do primeiro.
    private val tops: FloatArray = FloatArray(layouts.size).also { tops ->
        var y = 0f
        layouts.forEachIndexed { i, layout ->
            tops[i] = y
            y += layout.height + VERSE_GAP
        }
    }

    private val maskPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }

    /**
     * Desenha o quadro [frame] dentro de [area]: o verso atual fica a um terço
     * da altura, e as bordas de cima e de baixo somem em degradê.
     */
    fun draw(canvas: Canvas, area: RectF, frame: LyricsFrame, color: Int) {
        if (layouts.isEmpty()) return
        val anchor = area.top + area.height() * ANCHOR
        fun offsetOf(index: Int) = if (index < 0) 0f else tops[index]
        val scroll = offsetOf(frame.previous) + (offsetOf(frame.current) - offsetOf(frame.previous)) * frame.progress
        val dim = ColorUtils.setAlphaComponent(color, DIM_ALPHA)

        val saved = canvas.saveLayer(area, null)
        canvas.clipRect(area)
        layouts.forEachIndexed { i, layout ->
            val y = anchor + tops[i] - scroll
            if (y + layout.height < area.top || y > area.bottom) return@forEachIndexed
            // O verso que entra acende aos poucos; o que sai apaga aos poucos.
            paint.color = when (i) {
                frame.current -> ColorUtils.blendARGB(dim, color, frame.progress)
                frame.previous -> ColorUtils.blendARGB(color, dim, frame.progress)
                else -> dim
            }
            canvas.save()
            canvas.translate(area.left, y)
            layout.draw(canvas)
            canvas.restore()
        }
        // Degradê nas bordas: o texto some antes de chegar ao limite.
        maskPaint.shader = LinearGradient(
            0f, area.top, 0f, area.bottom,
            intArrayOf(0x00000000, 0xFF000000.toInt(), 0xFF000000.toInt(), 0x00000000),
            floatArrayOf(0f, FADE_TOP, 1f - FADE_BOTTOM, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(area, maskPaint)
        canvas.restoreToCount(saved)
    }

    private companion object {
        const val LYRIC_TEXT_SIZE = 72f
        const val VERSE_GAP = 44f
        const val MAX_LINES_PER_VERSE = 3
        const val ANCHOR = 0.3f
        const val DIM_ALPHA = 0x66
        const val FADE_TOP = 0.14f
        const val FADE_BOTTOM = 0.2f
    }
}
