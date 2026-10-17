// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.library

import android.content.Context
import android.graphics.Bitmap
import com.boomlistener.core.model.Track

/** Acesso às músicas do aparelho. O resto do app só enxerga esta interface. */
interface LibraryRepository {

    /** Todas as músicas do aparelho, em ordem alfabética de título. */
    suspend fun getTracks(): List<Track>

    /**
     * A capa de uma música em tamanho próximo de [sizePx], ou null se ela não
     * tiver capa. Guarda em cache na memória.
     */
    suspend fun loadArtwork(trackUri: String, sizePx: Int): Bitmap?
}

/**
 * Ponto de acesso provisório enquanto o app não tem injeção de dependências
 * (Hilt). Sempre devolve a mesma instância, para o cache de capas valer no app todo.
 */
object LibraryData {
    @Volatile
    private var instance: LibraryRepository? = null

    fun repository(context: Context): LibraryRepository =
        instance ?: synchronized(this) {
            instance ?: MediaStoreLibraryRepository(context.applicationContext).also { instance = it }
        }
}
