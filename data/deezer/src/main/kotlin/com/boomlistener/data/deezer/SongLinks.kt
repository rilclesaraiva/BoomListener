// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.deezer

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
