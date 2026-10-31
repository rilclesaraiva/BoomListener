// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.model

/** Um artista num serviço externo (hoje, o Deezer). Só informação pública. */
data class ExternalArtist(
    val id: Long,
    val name: String,
    val pictureUrl: String?,
    val fans: Long,
    val link: String?,
)

enum class ReleaseType { Album, EP, Single, Compilation }

/** Um lançamento da discografia (álbum, EP, single…). */
data class Release(
    val id: Long,
    val title: String,
    val type: ReleaseType,
    val releaseDate: String?,
    val coverUrl: String?,
    val link: String?,
) {
    val year: String? get() = releaseDate?.take(4)?.takeIf { it.all(Char::isDigit) }
}

/** Uma música popular do artista no serviço externo. */
data class PopularTrack(
    val id: Long,
    val title: String,
    val albumTitle: String?,
    val coverUrl: String?,
    val durationMs: Long,
    val link: String?,
)

/** Tudo o que o perfil do artista mostra vindo de fora. */
data class ArtistProfile(
    val artist: ExternalArtist,
    val topTracks: List<PopularTrack>,
    val releases: List<Release>,
    val related: List<ExternalArtist>,
)

/** Um álbum (ou EP, single…) aberto, com as músicas dele. */
data class AlbumDetail(
    val release: Release,
    val artistName: String,
    val tracks: List<PopularTrack>,
)
