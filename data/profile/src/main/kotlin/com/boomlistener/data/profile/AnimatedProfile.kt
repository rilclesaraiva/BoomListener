// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Movie
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import kotlin.math.roundToInt

/** Vídeo vira GIF de no máximo este tempo (sem som). */
internal const val MAX_ANIMATION_MS = MAX_GIF_DURATION_MS

/**
 * Tamanho de cada quadro do GIF gerado: menor que a foto/capa parada, para
 * caber no limite mesmo com muitos quadros.
 */
private fun baseSize(kind: ProfileImage): Pair<Int, Int> = when (kind) {
    ProfileImage.Avatar -> 240 to 240
    ProfileImage.Banner -> 480 to 180
}

/**
 * Quantos quadros por segundo dá para usar num GIF de [durationMs] com
 * [pixelsPerFrame], para caber em [maxBytes]. Em média um GIF de vídeo gasta
 * ~0,4 byte por pixel de cada quadro; fica entre 6 e 15 quadros por segundo
 * (15 já roda liso; mais que isso só pesaria).
 */
internal fun framesPerSecondFor(durationMs: Long, pixelsPerFrame: Int, maxBytes: Int): Int {
    val pixelFrames = maxBytes / BYTES_PER_PIXEL
    val seconds = (durationMs / 1000.0).coerceAtLeast(1.0)
    return (pixelFrames / pixelsPerFrame / seconds).toInt().coerceIn(MIN_FPS, MAX_FPS)
}

private const val BYTES_PER_PIXEL = 0.4
internal const val MIN_FPS = 6
internal const val MAX_FPS = 15

/** De onde vêm os quadros (um GIF grande ou um vídeo). */
private interface FrameSource {
    val durationMs: Long
    fun frameAt(ms: Long, width: Int, height: Int): IntArray?
    fun release() {}
}

/**
 * GIF grande demais ou vídeo curto → GIF animado que cabe no limite: menor,
 * com menos quadros por segundo e no máximo 15 s. Tenta de novo menor se
 * ainda passar. null se não der para ler.
 */
internal fun animatedProfileGif(context: Context, uri: Uri, isVideo: Boolean, gifBytes: ByteArray?, kind: ProfileImage): ByteArray? {
    val source = if (isVideo) videoSource(context, uri) else gifBytes?.let(::gifSource)
    source ?: return null
    try {
        val (baseW, baseH) = baseSize(kind)
        val duration = source.durationMs.coerceIn(1, MAX_ANIMATION_MS)
        for (scale in listOf(1f, 0.8f, 0.65f, 0.5f)) {
            val w = (baseW * scale).roundToInt()
            val h = (baseH * scale).roundToInt()
            val fps = framesPerSecondFor(duration, w * h, MAX_GIF_BYTES)
            val step = 1000L / fps
            val times = (0L until duration step step).toList()
            // Paleta a partir de alguns quadros espalhados pelo vídeo.
            val samples = times.filterIndexed { i, _ -> i % (times.size / 8).coerceAtLeast(1) == 0 }
                .take(8)
                .mapNotNull { source.frameAt(it, w, h) }
            if (samples.isEmpty()) return null
            val encoder = GifEncoder(w, h, GifEncoder.buildPalette(samples), delayCs = (100f / fps).roundToInt())
            var tooBig = false
            for (t in times) {
                source.frameAt(t, w, h)?.let(encoder::addFrame)
                if (encoder.size > MAX_GIF_BYTES) {
                    tooBig = true
                    break
                }
            }
            if (!tooBig) return encoder.finish()
        }
        return null
    } finally {
        source.release()
    }
}

/** Recorta o centro de [bitmap] na proporção do destino e devolve os pixels. */
private fun cropToPixels(bitmap: Bitmap, width: Int, height: Int): IntArray {
    val crop = centerCrop(bitmap.width, bitmap.height, width, height)
    val target = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    Canvas(target).drawBitmap(
        bitmap,
        Rect(crop[0], crop[1], crop[0] + crop[2], crop[1] + crop[3]),
        Rect(0, 0, width, height),
        Paint(Paint.FILTER_BITMAP_FLAG),
    )
    val pixels = IntArray(width * height)
    target.getPixels(pixels, 0, width, 0, 0, width, height)
    target.recycle()
    return pixels
}

@Suppress("DEPRECATION") // Movie é o único jeito do Android de ler quadro a quadro um GIF.
private fun gifSource(bytes: ByteArray): FrameSource? {
    val movie = Movie.decodeByteArray(bytes, 0, bytes.size) ?: return null
    if (movie.width() <= 0 || movie.height() <= 0) return null
    val canvasBitmap = Bitmap.createBitmap(movie.width(), movie.height(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(canvasBitmap)
    val total = movie.duration().toLong().coerceAtLeast(1)
    return object : FrameSource {
        override val durationMs = total
        override fun frameAt(ms: Long, width: Int, height: Int): IntArray {
            movie.setTime((ms % total).toInt())
            canvasBitmap.eraseColor(0xFF000000.toInt())
            movie.draw(canvas, 0f, 0f)
            return cropToPixels(canvasBitmap, width, height)
        }
        override fun release() = canvasBitmap.recycle()
    }
}

private fun videoSource(context: Context, uri: Uri): FrameSource? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: return null
        object : FrameSource {
            override val durationMs = duration
            override fun frameAt(ms: Long, width: Int, height: Int): IntArray? {
                val us = ms * 1000
                // Já pede o quadro pequeno quando dá (bem mais rápido).
                val frame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    retriever.getScaledFrameAtTime(us, MediaMetadataRetriever.OPTION_CLOSEST, width * 2, height * 2)
                } else {
                    retriever.getFrameAtTime(us, MediaMetadataRetriever.OPTION_CLOSEST)
                } ?: return null
                return cropToPixels(frame, width, height).also { frame.recycle() }
            }
            override fun release() = retriever.release()
        }
    } catch (_: Exception) {
        retriever.release()
        null
    }
}
