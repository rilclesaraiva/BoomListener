// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.deezer

import org.json.JSONObject

/*
 * Links para compartilhar: do Deezer para o YouTube Music, pelo song.link
 * (Odesli). O app nunca toca nada de lá; só monta o link que o usuário manda
 * para alguém (ver CLAUDE.md).
 */

private val VIDEO_ID = Regex("""[?&]v=([A-Za-z0-9_-]{11})""")
private val SHORT_ID = Regex("""youtu\.be/([A-Za-z0-9_-]{11})""")

/**
 * Link do YouTube Music no formato "music.youtube.com/watch?v=ID&drli=1",
 * que abre direto no app de quem recebe. Links de álbum/playlist do YouTube
 * Music ficam como vieram. null se não for um link do YouTube.
 */
fun youtubeMusicShareUrl(raw: String): String? {
    val id = VIDEO_ID.find(raw)?.groupValues?.get(1) ?: SHORT_ID.find(raw)?.groupValues?.get(1)
    if (id != null) return "https://music.youtube.com/watch?v=$id&drli=1"
    return raw.takeIf { it.startsWith("https://music.youtube.com/") }
}

/**
 * Músicas da busca do iTunes (catálogo da Apple Music), no mesmo formato das
 * do Deezer, para passar pela mesma comparação. O link é o da Apple Music,
 * que o song.link converte para o YouTube Music como faz com o do Deezer.
 */
internal fun parseItunesSongs(json: JSONObject): List<TrackCandidate> {
    val results = json.optJSONArray("results") ?: return emptyList()
    return (0 until results.length()).mapNotNull { index ->
        val item = results.optJSONObject(index) ?: return@mapNotNull null
        if (item.optString("kind") != "song") return@mapNotNull null
        val title = item.optString("trackName").takeIf { it.isNotBlank() } ?: return@mapNotNull null
        TrackCandidate(
            trackId = item.optLong("trackId"),
            albumId = item.optLong("collectionId"),
            title = title,
            artist = item.optString("artistName"),
            album = item.optString("collectionName"),
            coverUrl = item.optString("artworkUrl100").takeIf { it.isNotBlank() },
            durationMs = item.optLong("trackTimeMillis"),
            link = item.optString("trackViewUrl").takeIf { it.isNotBlank() },
        )
    }
}

/** A música que vai para o compartilhamento: só com boa certeza (melhor a busca do que a música errada). */
internal fun bestShareCandidate(local: LocalTrackInfo, candidates: List<TrackCandidate>): TrackCandidate? =
    rankCandidates(local, candidates, limit = 1).firstOrNull()?.takeIf { it.confidence >= SHARE_CONFIDENCE && it.link != null }

/** Confiança mínima para mandar o link exato. */
internal const val SHARE_CONFIDENCE = 0.6f
