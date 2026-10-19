// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.social

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import java.io.ByteArrayOutputStream

/** Tamanho máximo do JPEG enviado (o servidor aceita até 400 KB). */
private const val MAX_BYTES = 380_000

/** GIF animado vai como está, até este tamanho (o servidor aceita até 1,5 MB). */
internal const val MAX_GIF_BYTES = 1_450_000

/** O arquivo é um GIF? (pelos primeiros bytes) */
internal fun isGif(bytes: ByteArray): Boolean =
    bytes.size >= 6 && String(bytes, 0, 6, Charsets.US_ASCII).let { it == "GIF87a" || it == "GIF89a" }

/**
 * Lê a imagem escolhida (GIF animado pequeno vai como está), recorta no formato certo (quadrada para a foto,
 * faixa larga para a capa), reduz e devolve um JPEG pequeno. null se não der
 * para ler.
 */
internal fun prepareProfileImage(context: Context, uri: String, kind: ProfileImage): ByteArray? = runCatching {
    val parsed = Uri.parse(uri)
    // GIF animado pequeno vai inteiro (a animação se perderia se fosse recortado).
    // Grande demais: vira uma imagem parada, como as outras.
    if (context.contentResolver.getType(parsed) == "image/gif") {
        val bytes = context.contentResolver.openInputStream(parsed)?.use { it.readBytes() }
        if (bytes != null && isGif(bytes) && bytes.size <= MAX_GIF_BYTES) return bytes
    }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val sample = sampleSize(bounds.outWidth, bounds.outHeight, kind.width, kind.height)
    val source = context.contentResolver.openInputStream(parsed)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: return null
    val crop = centerCrop(source.width, source.height, kind.width, kind.height)
    val target = Bitmap.createBitmap(kind.width, kind.height, Bitmap.Config.ARGB_8888)
    Canvas(target).drawBitmap(
        source,
        Rect(crop[0], crop[1], crop[0] + crop[2], crop[1] + crop[3]),
        Rect(0, 0, kind.width, kind.height),
        Paint(Paint.FILTER_BITMAP_FLAG),
    )
    source.recycle()
    var quality = 85
    var bytes: ByteArray
    do {
        bytes = ByteArrayOutputStream().also { target.compress(Bitmap.CompressFormat.JPEG, quality, it) }.toByteArray()
        quality -= 10
    } while (bytes.size > MAX_BYTES && quality >= 35)
    target.recycle()
    bytes.takeIf { it.size <= MAX_BYTES }
}.getOrNull()

/**
 * O pedaço do centro da imagem com a proporção de [targetWidth]×[targetHeight]:
 * devolve [x, y, largura, altura].
 */
internal fun centerCrop(width: Int, height: Int, targetWidth: Int, targetHeight: Int): IntArray {
    val targetRatio = targetWidth.toDouble() / targetHeight
    return if (width.toDouble() / height > targetRatio) {
        // Imagem mais larga: corta as laterais.
        val cropWidth = (height * targetRatio).toInt().coerceIn(1, width)
        intArrayOf((width - cropWidth) / 2, 0, cropWidth, height)
    } else {
        // Imagem mais alta: corta em cima e embaixo.
        val cropHeight = (width / targetRatio).toInt().coerceIn(1, height)
        intArrayOf(0, (height - cropHeight) / 2, width, cropHeight)
    }
}

/** Fator de redução na leitura (potência de 2), sem ficar menor que o destino. */
internal fun sampleSize(width: Int, height: Int, targetWidth: Int, targetHeight: Int): Int {
    var sample = 1
    while (width / (sample * 2) >= targetWidth && height / (sample * 2) >= targetHeight) sample *= 2
    return sample
}
