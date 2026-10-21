// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.social

import android.content.Context
import com.boomlistener.core.common.ExternalLinks
import com.boomlistener.core.common.normalizedForSearch
import com.boomlistener.core.model.Track
import com.boomlistener.data.deezer.DeezerData
import com.boomlistener.data.deezer.DeezerRepository
import com.boomlistener.data.deezer.LocalTrackInfo
import com.boomlistener.data.deezer.TrackSearchResult
import com.boomlistener.data.social.MusicCard
import com.boomlistener.data.social.MusicKind

/** Confiança mínima para usar capa e link do Deezer num cartão. */
private const val CARD_CONFIDENCE = 0.6f

/**
 * Monta o cartão de uma música do aparelho: título, artista e álbum das tags
 * e, se o Deezer achar com segurança, a capa e o link do YouTube Music. Nunca
 * vai o arquivo.
 */
internal suspend fun buildTrackCard(track: Track, deezer: DeezerRepository): MusicCard {
    val found = runCatching {
        deezer.searchTrack(LocalTrackInfo(track.title, track.artist, track.album, track.durationMs))
    }.getOrNull() as? TrackSearchResult.Found
    val best = found?.candidates?.firstOrNull()?.takeIf { it.confidence >= CARD_CONFIDENCE }
    val link = best?.link?.let { runCatching { deezer.youtubeMusicLink(it) }.getOrNull() }
        ?: ExternalLinks.youtubeMusicSearch(track.title, track.artist)
    return MusicCard(
        kind = MusicKind.Track,
        title = track.title,
        artist = track.artist,
        album = track.album,
        coverUrl = best?.coverUrl,
        link = link,
    )
}

/**
 * A música do cartão que você já tem no aparelho (mesmo título e artista,
 * sem diferença de acento ou maiúscula), ou null.
 */
internal fun findLocalMatch(card: MusicCard, library: List<Track>): Track? {
    if (card.kind != MusicKind.Track) return null
    val title = card.title.normalizedForSearch()
    val artist = card.artist?.normalizedForSearch()
    return library.firstOrNull { track ->
        track.title.normalizedForSearch() == title &&
            (artist == null || track.artist?.normalizedForSearch()?.startsWith(artist) == true)
    }
}

/**
 * A capa (do Deezer) da música que está tocando, para o "ouvindo agora" dos
 * amigos: eles não têm o arquivo, então a capa de dentro dele não serve. Uma
 * busca por música, guardada na memória (tocar de novo não pergunta outra vez).
 */
object NowPlayingCovers {
    private const val KEEP = 200
    private val cache = object : LinkedHashMap<String, String>(KEEP, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > KEEP
    }

    /** "" no cache = já procurou e não achou. */
    suspend fun find(context: Context, title: String, artist: String?, album: String?): String? {
        val key = listOf(title, artist.orEmpty(), album.orEmpty()).joinToString("\u0000") { it.normalizedForSearch() }
        synchronized(cache) { cache[key] }?.let { return it.ifEmpty { null } }
        val found = runCatching {
            DeezerData.repository(context).searchTrack(LocalTrackInfo(title, artist, album, 0))
        }.getOrNull() ?: return null
        val cover = (found as? TrackSearchResult.Found)?.candidates?.firstOrNull()
            ?.takeIf { it.confidence >= CARD_CONFIDENCE }?.coverUrl
        // Falha de rede não fica guardada (tenta de novo na próxima).
        if (found is TrackSearchResult.Found || found is TrackSearchResult.NotFound) {
            synchronized(cache) { cache[key] = cover.orEmpty() }
        }
        return cover
    }
}
