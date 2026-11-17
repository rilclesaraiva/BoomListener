// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.library

private const val HOUR_MS = 3_600_000L
private const val MINUTE_MS = 60_000L

/** Tempo total da biblioteca, pronto para virar texto: horas inteiras (arredondadas) ou, abaixo de 1 hora, minutos. */
internal data class LibraryTotalTime(val hours: Int, val minutes: Int) {
    val inHours: Boolean get() = hours > 0
}

/** 90 min → 2 h (arredonda para o mais próximo); 40 min → 40 min; 0 → 0 min. */
internal fun libraryTotalTime(durationMs: Long): LibraryTotalTime {
    val ms = durationMs.coerceAtLeast(0L)
    if (ms < HOUR_MS) return LibraryTotalTime(0, (ms / MINUTE_MS).toInt())
    return LibraryTotalTime(((ms + HOUR_MS / 2) / HOUR_MS).toInt(), 0)
}
