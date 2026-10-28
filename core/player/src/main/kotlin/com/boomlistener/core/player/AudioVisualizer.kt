// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.player

import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger

/**
 * O som que está tocando, para o visualizador sobre a capa. Lido do próprio
 * player (a saída de som do app, [BoomAudioSink]), sem a API `Visualizer` do
 * Android, que exige a permissão de microfone.
 *
 * Cada pedaço analisado guarda o momento da música a que pertence, e a tela
 * mostra o do ponto que está saindo no fone agora (a saída recebe o som um
 * pouco antes de tocá-lo). Só analisa enquanto há um visualizador na tela
 * ([attach]/[detach]), para não gastar bateria à toa.
 */
object AudioVisualizer {
    // Quantos visualizadores estão na tela agora (só analisa com algum).
    private val viewers = AtomicInteger(0)
    private val active: Boolean get() = viewers.get() > 0

    // Osciloscópios na tela (guardam o som estéreo cru, não as barras).
    private val scopeViewers = AtomicInteger(0)
    private val scopeActive: Boolean get() = scopeViewers.get() > 0
    private val scopeLock = Any()

    // Criado só quando um osciloscópio aparece (≈ 2 MB).
    private var stereo: StereoHistory? = null

    /** A tela avisa que um visualizador apareceu ([detach] quando sair). */
    fun attach() {
        viewers.incrementAndGet()
    }

    fun detach() {
        viewers.updateAndGet { (it - 1).coerceAtLeast(0) }
    }

    /** Um osciloscópio apareceu ([detachScope] quando sair). */
    fun attachScope() {
        synchronized(scopeLock) { if (stereo == null) stereo = StereoHistory() }
        scopeViewers.incrementAndGet()
    }

    fun detachScope() {
        scopeViewers.updateAndGet { (it - 1).coerceAtLeast(0) }
    }

    /**
     * A amostra estéreo que está saindo no fone agora (contagem que só sobe),
     * ou -1 se ainda não há som guardado. Use com [scopeCopy].
     */
    fun scopeFrameNow(): Long {
        val base = positionUs
        if (base == C.TIME_UNSET) return -1
        val now = if (playing) base + (SystemClock.elapsedRealtimeNanos() - positionAtNanos) / 1000 else base
        return synchronized(scopeLock) { stereo?.frameAt(now) ?: -1 }
    }

    /** Copia as amostras de [from] até [to] (esquerdo e direito, de -1 a 1); devolve quantas. */
    fun scopeCopy(from: Long, to: Long, outLeft: FloatArray, outRight: FloatArray): Int =
        synchronized(scopeLock) { stereo?.copy(from, to, outLeft, outRight) ?: 0 }

    private const val HISTORY = 96
    private val lock = Any()
    private val times = LongArray(HISTORY) { Long.MIN_VALUE }
    private val bands = Array(HISTORY) { FloatArray(VISUALIZER_BANDS) }
    private val waves = Array(HISTORY) { FloatArray(VISUALIZER_WAVE_POINTS) }
    private var next = 0

    // Formato do som que chega na saída.
    private var sampleRate = 0
    private var channels = 0
    private var encoding = C.ENCODING_INVALID

    // Pedaço sendo juntado (mono).
    private val window = FloatArray(VISUALIZER_WINDOW)
    private var filled = 0
    private var windowStartUs = 0L
    private var lastBufferUs = Long.MIN_VALUE

    // Ponto que está tocando agora (no relógio da saída de som).
    @Volatile private var positionUs = C.TIME_UNSET
    @Volatile private var positionAtNanos = 0L
    @Volatile private var playing = false

    /**
     * Copia para [outBands] e [outWave] o pedaço do que está tocando agora.
     * Falso se ainda não há nada (começou agora, pausado há muito, outro formato).
     */
    fun read(outBands: FloatArray, outWave: FloatArray): Boolean {
        val base = positionUs
        if (base == C.TIME_UNSET) return false
        val now = if (playing) base + (SystemClock.elapsedRealtimeNanos() - positionAtNanos) / 1000 else base
        synchronized(lock) {
            var best = -1
            for (i in 0 until HISTORY) {
                val t = times[i]
                if (t != Long.MIN_VALUE && t <= now && (best < 0 || t > times[best])) best = i
            }
            if (best < 0) return false
            bands[best].copyInto(outBands, endIndex = minOf(outBands.size, VISUALIZER_BANDS))
            waves[best].copyInto(outWave, endIndex = minOf(outWave.size, VISUALIZER_WAVE_POINTS))
            return true
        }
    }

    // ---- Chamadas pela saída de som (thread do player) ----------------------

    @OptIn(UnstableApi::class)
    internal fun onConfigure(format: Format) {
        val raw = format.sampleMimeType == MimeTypes.AUDIO_RAW
        sampleRate = if (raw) format.sampleRate else 0
        channels = if (raw) format.channelCount else 0
        encoding = if (raw) format.pcmEncoding else C.ENCODING_INVALID
        onFlush()
    }

    internal fun onFlush() {
        filled = 0
        lastBufferUs = Long.MIN_VALUE
        synchronized(lock) { times.fill(Long.MIN_VALUE) }
        synchronized(scopeLock) { stereo?.clearMarks() }
    }

    internal fun onPosition(us: Long) {
        positionUs = us
        positionAtNanos = SystemClock.elapsedRealtimeNanos()
    }

    internal fun onPlaying(isPlaying: Boolean) {
        playing = isPlaying
    }

    /** Um pedaço de som decodificado; pode chegar repetido se a saída não aceitou tudo de uma vez. */
    internal fun onBuffer(buffer: ByteBuffer, presentationTimeUs: Long) {
        val wantBands = active
        val scope = scopeActive
        if ((!wantBands && !scope) || presentationTimeUs == lastBufferUs) return
        lastBufferUs = presentationTimeUs
        val rate = sampleRate
        val count = channels
        val bytesPerSample = when (encoding) {
            C.ENCODING_PCM_16BIT -> 2
            C.ENCODING_PCM_24BIT -> 3
            C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> 4
            else -> return
        }
        if (rate <= 0 || count <= 0) return
        val data = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        val frameBytes = bytesPerSample * count
        val frames = data.remaining() / frameBytes
        if (scope) storeStereo(data, frames, frameBytes, bytesPerSample, count, presentationTimeUs, rate)
        if (!wantBands) return
        var position = data.position()
        for (frame in 0 until frames) {
            var sum = 0f
            for (c in 0 until count) {
                sum += sampleAt(data, position, encoding)
                position += bytesPerSample
            }
            if (filled == 0) windowStartUs = presentationTimeUs + frame * 1_000_000L / rate
            window[filled++] = sum / count
            if (filled == VISUALIZER_WINDOW) {
                store(windowStartUs + VISUALIZER_WINDOW * 500_000L / rate, rate)
                filled = 0
            }
        }
    }

    private fun storeStereo(
        data: ByteBuffer,
        frames: Int,
        frameBytes: Int,
        bytesPerSample: Int,
        count: Int,
        timeUs: Long,
        rate: Int,
    ) {
        synchronized(scopeLock) {
            val history = stereo ?: return
            history.mark(timeUs, rate)
            var position = data.position()
            for (frame in 0 until frames) {
                val left = sampleAt(data, position, encoding)
                // Som mono: o mesmo nos dois lados (uma linha na diagonal, como num osciloscópio de verdade).
                val right = if (count > 1) sampleAt(data, position + bytesPerSample, encoding) else left
                history.push(left.coerceIn(-1f, 1f), right.coerceIn(-1f, 1f))
                position += frameBytes
            }
        }
    }

    private fun sampleAt(data: ByteBuffer, at: Int, encoding: Int): Float = when (encoding) {
        C.ENCODING_PCM_16BIT -> data.getShort(at) / 32768f
        C.ENCODING_PCM_24BIT -> {
            val value = (data.get(at).toInt() and 0xFF) or
                ((data.get(at + 1).toInt() and 0xFF) shl 8) or
                (data.get(at + 2).toInt() shl 16)
            value / 8_388_608f
        }
        C.ENCODING_PCM_32BIT -> data.getInt(at) / 2_147_483_648f
        else -> data.getFloat(at)
    }

    private fun store(timeUs: Long, rate: Int) {
        val newBands = visualizerBands(window, rate)
        val newWave = waveformPoints(window)
        synchronized(lock) {
            times[next] = timeUs
            newBands.copyInto(bands[next])
            newWave.copyInto(waves[next])
            next = (next + 1) % HISTORY
        }
    }
}
