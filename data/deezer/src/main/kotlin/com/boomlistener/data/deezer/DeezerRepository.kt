// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.deezer

import android.content.Context
import android.graphics.Bitmap
import com.boomlistener.core.model.AlbumDetail
import com.boomlistener.core.model.ArtistProfile

sealed interface AlbumResult {
    data class Found(val album: AlbumDetail) : AlbumResult
    data object Failed : AlbumResult
}

sealed interface ProfileResult {
    data class Found(val profile: ArtistProfile) : ProfileResult
    data object NotFound : ProfileResult
    data object Failed : ProfileResult
}

/** Informações públicas do Deezer (sem chave). Nunca toca música de lá. */
interface DeezerRepository {
    /** Acha o artista pelo nome que está nas tags e monta o perfil. */
    suspend fun artistProfile(artistName: String): ProfileResult

    /** Um álbum do Deezer com a lista de músicas. */
    suspend fun album(albumId: Long): AlbumResult

    /** Baixa (ou pega do cache) uma imagem do Deezer em ~[sizePx]. */
    suspend fun image(url: String, sizePx: Int): Bitmap?
}

/** Ponto de acesso provisório enquanto o app não tem Hilt. */
object DeezerData {
    @Volatile
    private var instance: DeezerRepository? = null

    fun repository(context: Context): DeezerRepository =
        instance ?: synchronized(this) {
            instance ?: HttpDeezerRepository(context.applicationContext).also { instance = it }
        }
}
