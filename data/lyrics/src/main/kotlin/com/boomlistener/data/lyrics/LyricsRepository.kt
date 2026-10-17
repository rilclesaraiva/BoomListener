// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.lyrics

import android.content.Context
import com.boomlistener.core.model.Lyrics

/** O que se sabe da música para procurar a letra. */
data class LyricsQuery(
    val title: String,
    val artist: String?,
    val album: String?,
    val durationMs: Long,
)

sealed interface LyricsResult {
    data class Found(val lyrics: Lyrics) : LyricsResult
    data object NotFound : LyricsResult
    /** Sem internet ou serviço fora do ar (dá para tentar de novo depois). */
    data object Failed : LyricsResult
}

/** Letras de músicas. Guarda o que encontra no aparelho. */
interface LyricsRepository {
    suspend fun getLyrics(query: LyricsQuery): LyricsResult
}

/** Ponto de acesso provisório enquanto o app não tem Hilt. */
object LyricsData {
    @Volatile
    private var instance: LyricsRepository? = null

    fun repository(context: Context): LyricsRepository =
        instance ?: synchronized(this) {
            instance ?: LrclibLyricsRepository(context.applicationContext).also { instance = it }
        }
}
