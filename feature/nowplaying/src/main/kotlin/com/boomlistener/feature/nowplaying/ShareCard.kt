// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.graphics.ColorUtils
import kotlin.math.abs
import kotlin.random.Random

/*
 * O banner de "estou ouvindo": uma imagem em pé (1080 × 1920, o tamanho dos
 * stories e status) com a capa grande, o nome da música, o artista, uma
 * onda de barrinhas na cor de destaque e o nome do app embaixo.
 *
 * É desenhado direto num Bitmap (não é uma tela): o mesmo desenho serve de
 * prévia no painel, de imagem compartilhada e de quadro do vídeo. As cores
 * vêm de quem chama (tema do app ou cor da capa), nunca daqui.
 */

/** Cores do banner (ARGB). */
internal data class ShareCardColors(
    val background: Int,
    val content: Int,
    val accent: Int,
)

/** Textos do banner, já no idioma do app. */
internal data class ShareCardText(
    val label: String,
    val title: String,
    val artist: String,
    val appName: String,
)

internal const val SHARE_CARD_WIDTH = 1080
internal const val SHARE_CARD_HEIGHT = 1920

/** O banner desenhado e onde ficam as barrinhas (para o vídeo redesenhá-las a cada quadro). */
internal class ShareCardImage(val bitmap: Bitmap, val waveCenterY: Float)

/**
 * Desenha o banner. [bars] são as alturas das barrinhas (0–1, do som do
 * trecho; nulo enquanto não carregou, ou sem leitura: barras genéricas).
 * Com [drawBars] falso o lugar delas fica vazio (o vídeo as desenha por cima).
 */
internal fun renderShareCard(
    cover: Bitmap?,
    text: ShareCardText,
    colors: ShareCardColors,
    bars: FloatArray?,
    drawBars: Boolean = true,
): ShareCardImage {
    val bitmap = Bitmap.createBitmap(SHARE_CARD_WIDTH, SHARE_CARD_HEIGHT, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val w = SHARE_CARD_WIDTH.toFloat()
    val h = SHARE_CARD_HEIGHT.toFloat()

    // Fundo: a cor escolhida em cima, escurecendo um pouco até embaixo.
    val bottom = ColorUtils.blendARGB(colors.background, android.graphics.Color.BLACK, 0.35f)
    canvas.drawRect(
        0f, 0f, w, h,
        Paint().apply { shader = LinearGradient(0f, 0f, 0f, h, colors.background, bottom, Shader.TileMode.CLAMP) },
    )

    val variant = ColorUtils.setAlphaComponent(colors.content, 0xB3)
    val regular = Typeface.create("sans-serif", Typeface.NORMAL)
    val bold = Typeface.create("sans-serif", Typeface.BOLD)

    // Rótulo no topo ("Ouvindo agora").
    val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = variant
        textSize = 40f
        typeface = bold
        letterSpacing = 0.12f
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(text.label.uppercase(), w / 2, 330f, labelPaint)

    // Capa com cantos arredondados e uma sombra suave.
    val size = 780f
    val left = (w - size) / 2
    val top = 420f
    val rect = RectF(left, top, left + size, top + size)
    val radius = 56f
    val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colors.background
        setShadowLayer(48f, 0f, 20f, ColorUtils.setAlphaComponent(android.graphics.Color.BLACK, 0x66))
    }
    canvas.drawRoundRect(rect, radius, radius, shadow)
    if (cover != null) {
        val shader = BitmapShader(cover, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val scale = maxOf(size / cover.width, size / cover.height)
        shader.setLocalMatrix(
            Matrix().apply {
                setScale(scale, scale)
                postTranslate(left + (size - cover.width * scale) / 2, top + (size - cover.height * scale) / 2)
            },
        )
        canvas.drawRoundRect(rect, radius, radius, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { this.shader = shader })
    } else {
        // Sem capa: a inicial do nome sobre a cor de destaque.
        canvas.drawRoundRect(rect, radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.accent })
        val initial = text.title.trim().firstOrNull()?.uppercase() ?: "♪"
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.background
            textSize = 360f
            typeface = bold
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(initial, rect.centerX(), rect.centerY() - (paint.ascent() + paint.descent()) / 2, paint)
    }

    // Nome da música (até 2 linhas) e artista.
    val textWidth = (w - 2 * 120f).toInt()
    var y = top + size + 80f
    val titleLayout = centeredLayout(text.title, textWidth, 2, TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colors.content
        textSize = 76f
        typeface = bold
    })
    canvas.save()
    canvas.translate(120f, y)
    titleLayout.draw(canvas)
    canvas.restore()
    y += titleLayout.height + 20f
    val artistLayout = centeredLayout(text.artist, textWidth, 1, TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = variant
        textSize = 50f
        typeface = regular
    })
    canvas.save()
    canvas.translate(120f, y)
    artistLayout.draw(canvas)
    canvas.restore()
    y += artistLayout.height + 70f

    // Barrinhas: o desenho do som do trecho.
    val waveCenterY = y + 50f
    if (drawBars) {
        drawWaveBars(canvas, w / 2, waveCenterY, bars ?: genericBars(text.title.hashCode() * 31 + text.artist.hashCode()), colors.accent)
    }

    // Nome do app embaixo.
    val appPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = variant
        textSize = 42f
        typeface = bold
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(text.appName, w / 2, h - 150f, appPaint)
    return ShareCardImage(bitmap, waveCenterY)
}

private fun centeredLayout(text: String, width: Int, maxLines: Int, paint: TextPaint): StaticLayout =
    StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
        .setAlignment(Layout.Alignment.ALIGN_CENTER)
        .setMaxLines(maxLines)
        .setEllipsize(TextUtils.TruncateAt.END)
        .setIncludePad(false)
        .build()

/** Desenha as barrinhas centradas em ([centerX], [centerY]), com alturas de 0 a 1. */
internal fun drawWaveBars(canvas: Canvas, centerX: Float, centerY: Float, heights: FloatArray, color: Int) {
    val barWidth = 14f
    val gap = 12f
    val maxHeight = 110f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    val total = heights.size * barWidth + (heights.size - 1) * gap
    var x = centerX - total / 2
    for (fraction in heights) {
        val height = (maxHeight * fraction).coerceAtLeast(barWidth)
        canvas.drawRoundRect(x, centerY - height / 2, x + barWidth, centerY + height / 2, barWidth / 2, barWidth / 2, paint)
        x += barWidth + gap
    }
}

/** Sem leitura do som: mais altas no meio, com um pouco de acaso (sempre iguais para a mesma música). */
private fun genericBars(seed: Int): FloatArray {
    val random = Random(seed)
    return FloatArray(WAVE_BARS) { i ->
        val middle = 1f - abs(i - WAVE_BARS / 2f) / (WAVE_BARS / 2f)
        0.25f + 0.55f * middle + 0.2f * random.nextFloat()
    }
}

/** Banner na cor da capa: o tom dela, escurecido para o texto claro ler bem. */
internal fun coverCardColors(seed: Int): ShareCardColors = ShareCardColors(
    background = ColorUtils.blendARGB(seed, android.graphics.Color.BLACK, 0.4f),
    content = android.graphics.Color.WHITE,
    accent = ColorUtils.blendARGB(seed, android.graphics.Color.WHITE, 0.45f),
)

/** Banner escuro, com só um toque da cor da capa. */
internal fun darkCardColors(seed: Int): ShareCardColors = ShareCardColors(
    background = ColorUtils.blendARGB(seed, android.graphics.Color.BLACK, 0.88f),
    content = android.graphics.Color.WHITE,
    accent = ColorUtils.blendARGB(seed, android.graphics.Color.WHITE, 0.3f),
)

/** Nome completo do app (nome próprio, igual em todo idioma). */
internal const val SHARE_CARD_APP_NAME = "Boom, listener!"
