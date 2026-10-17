// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.lyrics

import com.boomlistener.core.model.LyricLine

private val TIMESTAMP = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")
private val OFFSET = Regex("""\[offset:\s*([+-]?\d+)\s*]""", RegexOption.IGNORE_CASE)

/**
 * Lê uma letra no formato LRC ("[01:23.45] texto"). Aceita várias marcas de
 * tempo na mesma linha (refrão repetido), centésimos ou milésimos, a tag
 * [offset:] e ignora as tags de informação ([ar:], [ti:]…). Linhas vazias com
 * tempo viram pausas (texto vazio). Devolve em ordem de tempo.
 */
internal fun parseLrc(lrc: String): List<LyricLine> {
    val offsetMs = OFFSET.find(lrc)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
    val lines = mutableListOf<LyricLine>()
    for (raw in lrc.lineSequence()) {
        val stamps = TIMESTAMP.findAll(raw).toList()
        if (stamps.isEmpty()) continue
        val text = raw.substring(stamps.last().range.last + 1).trim()
        for (stamp in stamps) {
            val minutes = stamp.groupValues[1].toLong()
            val seconds = stamp.groupValues[2].toLong()
            val fraction = stamp.groupValues[3]
            val fractionMs = when (fraction.length) {
                0 -> 0L
                1 -> fraction.toLong() * 100
                2 -> fraction.toLong() * 10
                else -> fraction.take(3).toLong()
            }
            val time = (minutes * 60_000 + seconds * 1000 + fractionMs - offsetMs).coerceAtLeast(0)
            lines += LyricLine(time, text)
        }
    }
    return lines.sortedBy { it.timeMs }
}

/** Letra sem tempo: uma linha por linha do texto. */
internal fun parsePlain(text: String): List<LyricLine> =
    text.lines().map { LyricLine(0, it.trim()) }.dropWhile { it.text.isEmpty() }.dropLastWhile { it.text.isEmpty() }

/** Índice da linha que está tocando em [positionMs]; -1 antes da primeira. */
fun currentLyricIndex(lines: List<LyricLine>, positionMs: Long): Int {
    var low = 0
    var high = lines.lastIndex
    var answer = -1
    while (low <= high) {
        val mid = (low + high) / 2
        if (lines[mid].timeMs <= positionMs) {
            answer = mid
            low = mid + 1
        } else {
            high = mid - 1
        }
    }
    return answer
}
