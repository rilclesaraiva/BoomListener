// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

/**
 * O trecho da música que vai no vídeo do banner: no máximo 30 s (só um
 * pedaço, nunca a música inteira, por respeito aos direitos de quem fez).
 */
internal data class ShareClip(val startMs: Long, val endMs: Long) {
    val lengthMs: Long get() = endMs - startMs

    companion object {
        const val MAX_MS = 30_000L

        /** Até onde o começo do trecho pode ir (a música menos 30 s). */
        fun maxStart(durationMs: Long): Long = (durationMs - MAX_MS).coerceAtLeast(0)

        /** Trecho de 30 s começando em [startMs] (ajustado para caber na música). */
        fun at(startMs: Long, durationMs: Long): ShareClip {
            val start = startMs.coerceIn(0, maxStart(durationMs))
            val end = if (durationMs > 0) (start + MAX_MS).coerceAtMost(durationMs) else start + MAX_MS
            return ShareClip(start, end)
        }
    }
}
