// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.deezer

import android.content.Context
import android.graphics.Bitmap
import com.boomlistener.core.model.TagSuggestion
import com.boomlistener.core.model.AlbumDetail
import com.boomlistener.core.model.ArtistProfile

sealed interface AlbumResult {
    data class Found(val album: AlbumDetail) : AlbumResult
    data object Failed : AlbumResult
}

sealed interface TrackSearchResult {
    /** Do mais provável para o menos (ver [rankCandidates]). */
    data class Found(val candidates: List<TrackCandidate>) : TrackSearchResult
    data object NotFound : TrackSearchResult
    data object Failed : TrackSearchResult
}

sealed interface TrackTagsResult {
    data class Found(val suggestion: TagSuggestion) : TrackTagsResult
    data object Failed : TrackTagsResult
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

    /** Procura a música do arquivo no Deezer (tags automáticas). */
    suspend fun searchTrack(local: LocalTrackInfo): TrackSearchResult = TrackSearchResult.Failed

    /** Todas as tags oficiais de um resultado (faixa, disco, ano, gênero, capa…). */
    suspend fun trackTags(candidate: TrackCandidate): TrackTagsResult = TrackTagsResult.Failed

    /** A imagem original (para gravar como capa no arquivo). */
    suspend fun imageBytes(url: String): ByteArray? = null

    /**
     * Link do YouTube Music (formato de [youtubeMusicShareUrl]) para uma
     * música ou álbum do Deezer, pelo song.link. null se não achar.
     */
    suspend fun youtubeMusicLink(deezerUrl: String): String? = null

    /** O mesmo para uma música do aparelho: acha no Deezer primeiro. */
    suspend fun youtubeMusicLinkForSong(title: String, artist: String?, durationMs: Long): String? = null
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
