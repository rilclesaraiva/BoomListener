// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.nio.ByteOrder
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Lê a força do som do trecho: decodifica só esses 30 s do arquivo (com os
 * decodificadores do aparelho) e mede o RMS a cada [LEVEL_WINDOW_MS]. Nulo se
 * o arquivo não puder ser lido; aí o banner usa barras genéricas.
 */
internal suspend fun loadClipLevels(context: Context, uri: String, clip: ShareClip): FloatArray? =
    withContext(Dispatchers.IO) {
        runCatching { decodeLevels(context, uri, clip) }.getOrNull()
    }

private suspend fun decodeLevels(context: Context, uri: String, clip: ShareClip): FloatArray? {
    val count = ((clip.lengthMs + LEVEL_WINDOW_MS - 1) / LEVEL_WINDOW_MS).toInt().coerceAtLeast(1)
    val sums = DoubleArray(count)
    val samples = IntArray(count)
    val extractor = MediaExtractor()
    var codec: MediaCodec? = null
    try {
        extractor.setDataSource(context, Uri.parse(uri), null)
        val track = (0 until extractor.trackCount).firstOrNull {
            extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        } ?: return null
        extractor.selectTrack(track)
        val format = extractor.getTrackFormat(track)
        val startUs = clip.startMs * 1000
        val endUs = clip.endMs * 1000
        extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

        val decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        codec = decoder
        decoder.configure(format, null, null, 0)
        decoder.start()

        var outFormat = decoder.outputFormat
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        while (!outputDone) {
            currentCoroutineContext().ensureActive()
            if (!inputDone) {
                val inIndex = decoder.dequeueInputBuffer(TIMEOUT_US)
                if (inIndex >= 0) {
                    val buffer = decoder.getInputBuffer(inIndex)!!
                    val size = extractor.readSampleData(buffer, 0)
                    if (size < 0 || extractor.sampleTime > endUs) {
                        decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        decoder.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            val outIndex = decoder.dequeueOutputBuffer(info, TIMEOUT_US)
            when {
                outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> outFormat = decoder.outputFormat
                outIndex >= 0 -> {
                    val buffer = decoder.getOutputBuffer(outIndex)
                    if (buffer != null && info.size > 0) {
                        buffer.position(info.offset)
                        buffer.limit(info.offset + info.size)
                        val channels = outFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
                        val rate = outFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE).coerceAtLeast(1)
                        val float = outFormat.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
                            outFormat.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT
                        val ordered = buffer.order(ByteOrder.nativeOrder())
                        val values = if (float) ordered.asFloatBuffer().let { b -> FloatArray(b.remaining()).also(b::get) }
                        else ordered.asShortBuffer().let { b -> FloatArray(b.remaining()) { b.get(it) / 32768f } }
                        val frames = values.size / channels
                        for (frame in 0 until frames) {
                            val timeUs = info.presentationTimeUs + frame * 1_000_000L / rate
                            if (timeUs < startUs || timeUs >= endUs) continue
                            val window = ((timeUs - startUs) / 1000 / LEVEL_WINDOW_MS).toInt()
                            if (window !in 0 until count) continue
                            for (c in 0 until channels) {
                                val v = values[frame * channels + c]
                                sums[window] += (v * v).toDouble()
                            }
                            samples[window] += channels
                        }
                        if (info.presentationTimeUs > endUs) outputDone = true
                    }
                    decoder.releaseOutputBuffer(outIndex, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                }
            }
        }
    } finally {
        runCatching { codec?.stop() }
        runCatching { codec?.release() }
        extractor.release()
    }
    if (samples.all { it == 0 }) return null
    return FloatArray(count) { if (samples[it] == 0) 0f else sqrt(sums[it] / samples[it]).toFloat() }
}

private const val TIMEOUT_US = 10_000L
