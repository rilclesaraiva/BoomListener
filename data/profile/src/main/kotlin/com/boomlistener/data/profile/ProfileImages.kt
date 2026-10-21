// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Movie
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream

/** Tamanho máximo da imagem guardada (foto e capa ficam leves). */
private const val MAX_BYTES = 380_000

/** GIF animado é guardado como está até este tamanho; maior, é refeito menor. */
internal const val MAX_GIF_BYTES = 2_900_000

/** GIF mais longo que isto é refeito (menor e mais curto). */
internal const val MAX_GIF_DURATION_MS = 15_000L

/** Largura e altura de um GIF (do cabeçalho); null se não for GIF. */
internal fun gifSize(bytes: ByteArray): Pair<Int, Int>? {
    if (!isGif(bytes) || bytes.size < 10) return null
    val width = (bytes[6].toInt() and 0xFF) or ((bytes[7].toInt() and 0xFF) shl 8)
    val height = (bytes[8].toInt() and 0xFF) or ((bytes[9].toInt() and 0xFF) shl 8)
    return width to height
}

/**
 * O GIF pode ir como está? Só se for pequeno, não maior que o necessário
 * (com folga de 25%) e curto. Senão é refeito, sem perder a animação.
 */
internal fun gifFitsAsIs(sizeBytes: Int, width: Int, height: Int, durationMs: Long, kind: ProfileImage): Boolean =
    sizeBytes <= MAX_GIF_BYTES &&
        width <= kind.width * 5 / 4 &&
        height <= maxOf(kind.height, kind.width) * 5 / 4 &&
        durationMs <= MAX_GIF_DURATION_MS

/** O arquivo é um GIF? (pelos primeiros bytes) */
internal fun isGif(bytes: ByteArray): Boolean =
    bytes.size >= 6 && String(bytes, 0, 6, Charsets.US_ASCII).let { it == "GIF87a" || it == "GIF89a" }

/**
 * Lê a imagem escolhida (GIF pequeno vai como está; GIF grande e vídeo viram um GIF menor), recorta no formato certo (quadrada para a foto,
 * faixa larga para a capa), reduz e devolve um JPEG pequeno. null se não der
 * para ler.
 */
internal fun prepareProfileImage(context: Context, uri: String, kind: ProfileImage): ByteArray? = runCatching {
    val parsed = Uri.parse(uri)
    // GIF animado pequeno vai inteiro (a animação se perderia se fosse recortado).
    // Grande demais: vira uma imagem parada, como as outras.
    // Vídeo (até 15 s, sem som) vira GIF animado.
    if (context.contentResolver.getType(parsed)?.startsWith("video/") == true) {
        return animatedProfileGif(context, parsed, isVideo = true, gifBytes = null, kind = kind)
    }
    // GIF: reconhecido pelo conteúdo (o tipo informado pelo seletor nem sempre é "image/gif").
    // Pequeno vai como está; grande é refeito menor (sem perder a animação).
    val head = context.contentResolver.openInputStream(parsed)?.use { stream -> ByteArray(6).also { stream.read(it) } }
    if (head != null && isGif(head)) {
        val bytes = context.contentResolver.openInputStream(parsed)?.use { it.readBytes() }
        if (bytes != null) {
            val (width, height) = gifSize(bytes) ?: (0 to 0)
            if (gifFitsAsIs(bytes.size, width, height, gifDurationMs(bytes), kind)) return bytes
        }
        if (bytes != null) animatedProfileGif(context, parsed, isVideo = false, gifBytes = bytes, kind = kind)?.let { return it }
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
    // WebP: bem menor que o JPEG com a mesma qualidade.
    @Suppress("DEPRECATION")
    val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
    var quality = 82
    var bytes: ByteArray
    do {
        bytes = ByteArrayOutputStream().also { target.compress(format, quality, it) }.toByteArray()
        quality -= 10
    } while (bytes.size > MAX_BYTES && quality >= 32)
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

/** Duração de um GIF animado (0 se não der para ler). */
@Suppress("DEPRECATION") // Movie é o jeito do Android de ler a duração de um GIF.
private fun gifDurationMs(bytes: ByteArray): Long =
    runCatching { Movie.decodeByteArray(bytes, 0, bytes.size)?.duration()?.toLong() }.getOrNull() ?: 0L
