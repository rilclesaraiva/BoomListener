// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.ToInt16PcmAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.CanvasOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/*
 * Arquivos do banner para compartilhar: a imagem (PNG) e o vídeo (MP4 com o
 * banner parado e o trecho de até 30 s da música). Ficam no cache, na pasta
 * "share", que o FileProvider deixa o app escolhido ler. Cada novo banner
 * apaga os anteriores.
 */

private const val SHARE_DIR = "share"
private const val VIDEO_FRAME_RATE = 24

private fun shareDir(context: Context): File =
    File(context.cacheDir, SHARE_DIR).apply {
        deleteRecursively()
        mkdirs()
    }

/** Grava o banner como PNG. */
internal suspend fun saveShareImage(context: Context, card: Bitmap): File = withContext(Dispatchers.IO) {
    val file = File(shareDir(context), "boomlistener_${System.currentTimeMillis()}.png")
    file.outputStream().use { card.compress(Bitmap.CompressFormat.PNG, 100, it) }
    file
}

/**
 * Faz o vídeo: o banner durante o trecho, com o som da música desse trecho.
 * Com [spectrum], [card] vem sem as barrinhas e elas são desenhadas por cima a
 * cada quadro, como um equalizador ([spectrumBars]: graves à esquerda, agudos
 * à direita); sem, o banner fica parado.
 * Roda no Transformer do Media3 (que precisa da thread principal).
 */
@OptIn(UnstableApi::class)
internal suspend fun exportShareVideo(
    context: Context,
    card: ShareCardImage,
    spectrum: Array<FloatArray>?,
    barColor: Int,
    audioUri: String,
    clip: ShareClip,
): File {
    val image = saveShareImage(context, card.bitmap)
    val output = File(image.parentFile, image.nameWithoutExtension + ".mp4")
    val still = EditedMediaItem.Builder(
        MediaItem.Builder()
            .setUri(Uri.fromFile(image))
            .setMimeType(MimeTypes.IMAGE_PNG)
            .setImageDurationMs(clip.lengthMs)
            .build(),
    ).setFrameRate(VIDEO_FRAME_RATE)
        .apply {
            if (spectrum != null) {
                setEffects(Effects(emptyList(), listOf(OverlayEffect(listOf(PulseOverlay(spectrum, card.waveCenterY, barColor))))))
            }
        }
        .build()
    val audio = EditedMediaItem.Builder(
        MediaItem.Builder()
            .setUri(audioUri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(clip.startMs)
                    .setEndPositionMs(clip.endMs)
                    .build(),
            )
            .build(),
    )
        .setRemoveVideo(true)
        // Misturar sequências exige áudio em PCM de 16 bits (FLAC de 24 bits, por exemplo, não é).
        .setEffects(Effects(listOf(ToInt16PcmAudioProcessor()), emptyList()))
        .build()
    val composition = Composition.Builder(
        EditedMediaItemSequence.withVideoFrom(listOf(still)),
        EditedMediaItemSequence.withAudioFrom(listOf(audio)),
    ).build()

    return withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val transformer = Transformer.Builder(context.applicationContext)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        if (continuation.isActive) continuation.resume(output)
                    }

                    override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                        output.delete()
                        if (continuation.isActive) continuation.resumeWithException(exportException)
                    }
                })
                .build()
            transformer.start(composition, output.absolutePath)
            val main = Handler(Looper.getMainLooper())
            continuation.invokeOnCancellation {
                main.post {
                    transformer.cancel()
                    output.delete()
                }
            }
        }
    }
}

/** Abre o "compartilhar com…" do Android com o arquivo do banner. */
internal fun Context.shareFile(file: File, mimeType: String, chooserTitle: String) {
    val uri = FileProvider.getUriForFile(this, "$packageName.share", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri(null, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/**
 * As barrinhas do vídeo: a cada quadro, limpa e desenha as alturas daquele
 * instante do trecho, no mesmo lugar em que a imagem as desenharia.
 */
@OptIn(UnstableApi::class)
private class PulseOverlay(
    private val spectrum: Array<FloatArray>,
    private val centerY: Float,
    private val color: Int,
) : CanvasOverlay(true) {
    override fun onDraw(canvas: Canvas, presentationTimeUs: Long) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        // O quadro pode ter outro tamanho que o banner: desenha na escala dele.
        val scale = canvas.width.toFloat() / SHARE_CARD_WIDTH
        canvas.save()
        canvas.scale(scale, scale)
        drawWaveBars(canvas, SHARE_CARD_WIDTH / 2f, centerY, spectrumBars(spectrum, presentationTimeUs / 1000), color)
        canvas.restore()
    }
}
