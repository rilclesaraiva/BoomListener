// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.player

/**
 * As últimas amostras estéreo que chegaram à saída de som, para o
 * osciloscópio XY (esquerdo = horizontal, direito = vertical). Puro e testado.
 *
 * As amostras são contadas desde o começo ([written], nunca volta), e cada
 * pedaço que chega marca em que amostra começa um momento da música
 * ([mark]); assim [frameAt] acha a amostra que está tocando agora. Não é
 * seguro entre threads: quem usa segura uma trava.
 */
internal class StereoHistory(capacityPow2: Int = 1 shl 18, private val marks: Int = 512) {
    private val capacity = capacityPow2
    private val mask = capacityPow2 - 1
    private val left = FloatArray(capacity)
    private val right = FloatArray(capacity)

    init {
        require(capacityPow2 > 0 && capacityPow2 and mask == 0) { "capacidade precisa ser potência de 2" }
    }

    /** Quantas amostras já entraram (desde que o histórico existe). */
    var written = 0L
        private set

    private val markTimes = LongArray(marks) { Long.MIN_VALUE }
    private val markFrames = LongArray(marks)
    private val markRates = IntArray(marks)
    private var markNext = 0

    /** O que entrar a seguir começa no momento [timeUs] da música, a [sampleRate] amostras por segundo. */
    fun mark(timeUs: Long, sampleRate: Int) {
        if (sampleRate <= 0) return
        markTimes[markNext] = timeUs
        markFrames[markNext] = written
        markRates[markNext] = sampleRate
        markNext = (markNext + 1) % marks
    }

    fun push(l: Float, r: Float) {
        val i = (written and mask.toLong()).toInt()
        left[i] = l
        right[i] = r
        written++
    }

    /** Esquece as marcas (pulo na música, outra música): o que já entrou não vale mais. */
    fun clearMarks() {
        markTimes.fill(Long.MIN_VALUE)
    }

    /**
     * A amostra que toca no momento [timeUs] (contagem de [written]), ou -1
     * se não há marca até ali. Nunca passa do que já entrou.
     */
    fun frameAt(timeUs: Long): Long {
        var best = -1
        for (i in 0 until marks) {
            val t = markTimes[i]
            if (t != Long.MIN_VALUE && t <= timeUs && (best < 0 || t > markTimes[best])) best = i
        }
        if (best < 0) return -1
        val frame = markFrames[best] + (timeUs - markTimes[best]) * markRates[best] / 1_000_000L
        return frame.coerceAtMost(written)
    }

    /**
     * Copia as amostras de [from] até [to] (sem incluir [to]) para [outL] e
     * [outR]. Se forem mais do que cabem, ficam as mais recentes; o que já
     * saiu do histórico não volta. Devolve quantas copiou.
     */
    fun copy(from: Long, to: Long, outL: FloatArray, outR: FloatArray): Int {
        val end = minOf(to, written)
        val start = maxOf(from, written - capacity, end - minOf(outL.size, outR.size), 0L)
        if (end <= start) return 0
        val count = (end - start).toInt()
        for (k in 0 until count) {
            val i = ((start + k) and mask.toLong()).toInt()
            outL[k] = left[i]
            outR[k] = right[i]
        }
        return count
    }
}

/**
 * Brilho de um trecho do feixe do osciloscópio (0 a 1): o feixe passa pelo
 * trecho no mesmo tempo, qualquer que seja o tamanho, então trecho comprido
 * (salto rápido) fica fraco e trecho curto acende. É o que deixa os desenhos
 * da "oscilloscope music" limpos: os saltos entre uma forma e outra somem.
 */
fun beamIntensity(lengthPx: Float, focusPx: Float): Float {
    if (focusPx <= 0f) return 0f
    return (focusPx / maxOf(lengthPx, focusPx)).coerceIn(0f, 1f)
}

/** Níveis de brilho desenhados (cada um numa passada); abaixo do último, o trecho não aparece. */
val BEAM_LEVELS = floatArrayOf(1f, 0.5f, 0.25f, 0.12f)

/** Em que nível de [BEAM_LEVELS] cai um brilho, ou -1 se é fraco demais para aparecer. */
fun beamLevel(intensity: Float): Int {
    for (i in BEAM_LEVELS.indices) {
        val lower = if (i + 1 < BEAM_LEVELS.size) (BEAM_LEVELS[i] + BEAM_LEVELS[i + 1]) / 2 else BEAM_LEVELS[i] * 0.6f
        if (intensity >= lower) return i
    }
    return -1
}
