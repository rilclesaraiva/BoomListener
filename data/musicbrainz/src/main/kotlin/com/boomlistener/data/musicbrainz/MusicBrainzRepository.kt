// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.musicbrainz

import android.content.Context
import com.boomlistener.core.model.ArtistProfile

/**
 * Uma gravação do MusicBrainz já ligada ao lançamento mais provável (álbum
 * oficial, o mais antigo). Campos que o MusicBrainz não tem ficam vazios.
 */
data class MbRecording(
    val recordingId: String,
    val releaseId: String?,
    val releaseGroupId: String?,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val date: String,
    val trackNumber: String,
    val trackTotal: String,
    val discNumber: String,
    val discTotal: String,
) {
    /** Capa pequena (para a lista de opções). */
    val thumbnailUrl: String? get() = releaseId?.let { "$COVER_ART_URL/release/$it/front-250" }
    val link: String get() = "https://musicbrainz.org/recording/$recordingId"
}

sealed interface MbSearchResult {
    data class Found(val recordings: List<MbRecording>) : MbSearchResult
    data object NotFound : MbSearchResult
    data object Failed : MbSearchResult
}

sealed interface MbArtistResult {
    data class Found(val profile: ArtistProfile) : MbArtistResult
    data object NotFound : MbArtistResult
    data object Failed : MbArtistResult
}

/**
 * MusicBrainz + Cover Art Archive: bancos abertos e estáveis, usados como
 * reserva quando o Deezer não acha a música ou o artista. Só informação;
 * nunca toca nada. Sem chave, mas o MusicBrainz pede no máximo 1 consulta por
 * segundo e um User-Agent que identifique o app.
 */
interface MusicBrainzRepository {
    /** Procura a música (título + artista) para as tags automáticas. */
    suspend fun searchRecordings(title: String, artist: String?): MbSearchResult

    /** A capa do lançamento (ou, sem ela, a do álbum em qualquer edição), para gravar no arquivo. */
    suspend fun coverBytes(recording: MbRecording): ByteArray?

    /**
     * Perfil do artista (discografia com capas do Cover Art Archive). Entre
     * homônimos, fica o que tem mais álbuns em comum com [localAlbums].
     */
    suspend fun artistProfile(artistName: String, localAlbums: List<String>): MbArtistResult
}

/** Ponto de acesso provisório enquanto o app não tem Hilt. */
object MusicBrainzData {
    @Volatile
    private var instance: MusicBrainzRepository? = null

    fun repository(context: Context): MusicBrainzRepository =
        instance ?: synchronized(this) {
            instance ?: HttpMusicBrainzRepository(context.applicationContext).also { instance = it }
        }
}

internal const val COVER_ART_URL = "https://coverartarchive.org"
