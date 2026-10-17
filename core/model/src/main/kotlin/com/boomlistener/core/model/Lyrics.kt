// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.model

/** Uma linha da letra. [timeMs] é quando ela começa (0 em letra sem tempo). */
data class LyricLine(
    val timeMs: Long,
    val text: String,
)

/**
 * Letra de uma música. [synced] = cada linha tem o seu tempo (dá para
 * destacar a linha atual e pular para ela).
 */
data class Lyrics(
    val lines: List<LyricLine>,
    val synced: Boolean,
    /** De onde veio, para dar o crédito na tela (ex.: "LRCLIB"). */
    val source: String,
)
