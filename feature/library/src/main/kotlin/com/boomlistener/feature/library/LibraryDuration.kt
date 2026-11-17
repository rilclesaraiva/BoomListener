// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

private const val HOUR_MS = 3_600_000L
private const val MINUTE_MS = 60_000L

/** Tempo total da biblioteca, pronto para virar texto: horas e minutos inteiros (sem arredondar para cima). */
internal data class LibraryTotalTime(val hours: Int, val minutes: Int)

/** 10 h 24 min 40 s → 10 h 24 min (os segundos que sobram não contam); 40 min → 0 h 40 min; 0 → 0 min. */
internal fun libraryTotalTime(durationMs: Long): LibraryTotalTime {
    val ms = durationMs.coerceAtLeast(0L)
    return LibraryTotalTime((ms / HOUR_MS).toInt(), ((ms % HOUR_MS) / MINUTE_MS).toInt())
}
