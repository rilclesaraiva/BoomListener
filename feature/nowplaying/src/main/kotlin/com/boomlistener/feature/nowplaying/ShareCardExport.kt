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
import android.graphics.Rect
import android.graphics.Paint
import android.graphics.RectF
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
import com.boomlistener.core.player.smoothVisualizer
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
// 30 quadros por segundo: a onda e a letra correm mais lisas que a 24.
private const val VIDEO_FRAME_RATE = 30

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
 * à direita); sem, o banner fica parado. Com [lyrics] (banner com letra), a
 * letra rola na área dela, sincronizada com o trecho.
 * Roda no Transformer do Media3 (que precisa da thread principal).
 */
@OptIn(UnstableApi::class)
internal suspend fun exportShareVideo(
    context: Context,
    card: ShareCardImage,
    sound: ClipSound?,
    barColor: Int,
    audioUri: String,
    clip: ShareClip,
    lyrics: ShareLyricsLayout? = null,
    lyricsColor: Int = Color.WHITE,
    visualizer: ShareVisualizer = ShareVisualizer.Bars,
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
            val area = card.lyricsArea
            val moving = sound != null && visualizer != ShareVisualizer.None
            if (moving || (lyrics != null && area != null)) {
                val overlay = BannerOverlay(
                    card.bitmap, sound.takeIf { moving }, visualizer, card.waveCenterY, barColor,
                    lyrics?.takeIf { area != null }, area, lyricsColor, clip.startMs,
                )
                setEffects(Effects(emptyList(), listOf(OverlayEffect(listOf(overlay)))))
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
 * O que se mexe no vídeo, redesenhado a cada quadro no mesmo lugar em que a
 * imagem desenharia: as barrinhas do instante do trecho ([spectrum]) e a
 * letra rolando ([lyrics], no tempo da música: começo do trecho + quadro).
 */
@OptIn(UnstableApi::class)
private class BannerOverlay(
    /** O banner parado (o fundo de cada quadro). */
    private val base: Bitmap,
    private val sound: ClipSound?,
    private val visualizer: ShareVisualizer,
    private val centerY: Float,
    private val barColor: Int,
    private val lyrics: ShareLyricsLayout?,
    private val lyricsArea: RectF?,
    private val lyricsColor: Int,
    private val clipStartMs: Long,
) : CanvasOverlay(true) {
    // A onda mostrada no quadro anterior (os quadros vêm em ordem) e a do quadro atual.
    private val shownWave = FloatArray(SOUND_WAVE_POINTS)
    private val targetWave = FloatArray(SOUND_WAVE_POINTS)
    private val waveGain = sound?.let { soundWaveGain(it.mono) } ?: 1f
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val waveBand = Rect(0, (centerY - WAVE_BAND_HALF).toInt(), SHARE_CARD_WIDTH, (centerY + WAVE_BAND_HALF).toInt())
    private val lyricsBand = lyricsArea?.let { Rect(0, it.top.toInt(), SHARE_CARD_WIDTH, it.bottom.toInt()) }

    override fun onDraw(canvas: Canvas, presentationTimeUs: Long) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        // O quadro pode ter outro tamanho que o banner: desenha na escala dele.
        val scale = canvas.width.toFloat() / SHARE_CARD_WIDTH
        val clipMs = presentationTimeUs / 1000
        canvas.save()
        canvas.scale(scale, scale)
        // Por baixo do que se mexe, o próprio banner (opaco). Desenhado direto
        // sobre o transparente, o que é meio transparente (o brilho da onda,
        // os versos apagados) saía escuro no vídeo, com uma borda preta.
        canvas.drawBitmap(base, waveBand, waveBand, bitmapPaint)
        lyricsBand?.let { canvas.drawBitmap(base, it, it, bitmapPaint) }
        when {
            sound == null -> Unit
            visualizer == ShareVisualizer.Bars ->
                drawWaveBars(canvas, SHARE_CARD_WIDTH / 2f, centerY, spectrumBars(sound.spectrum, clipMs), barColor)
            visualizer == ShareVisualizer.Wave -> {
                // Tirada das amostras no instante exato do quadro e suavizada de um
                // quadro para o outro, como o visualizador do player.
                soundWaveAtTime(sound.mono, sound.sampleRate, waveGain, clipMs, targetWave)
                for (i in shownWave.indices) shownWave[i] = smoothVisualizer(shownWave[i], targetWave[i], rise = 0.7f, fall = 0.45f)
                drawSoundWave(canvas, SHARE_CARD_WIDTH / 2f, centerY, shownWave, barColor)
            }
        }
        if (lyrics != null && lyricsArea != null) {
            lyrics.draw(canvas, lyricsArea, shareLyricsFrame(lyrics.times, clipStartMs + clipMs), lyricsColor)
        }
        canvas.restore()
    }

    private companion object {
        /** Meia altura da faixa da onda e das barrinhas (com o brilho). */
        const val WAVE_BAND_HALF = 170f
    }
}
