// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

/**
 * Links "abrir em…". O app nunca toca nada de fora: só leva o usuário até lá
 * (ver CLAUDE.md).
 */
object ExternalLinks {
    /** Busca no YouTube Music por "artista título" (ou só o título). */
    fun youtubeMusicSearch(title: String, artist: String?): String {
        val query = listOfNotNull(artist, title).joinToString(" ")
        return "https://music.youtube.com/search?q=" + URLEncoder.encode(query, "UTF-8")
    }
}

/** Abre um link no app que cuidar dele (navegador, Deezer, YT Music…). */
fun Context.openUrl(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // Nenhum app abre esse link: não há o que fazer.
    }
}

/** Abre a folha de compartilhar do Android com [text]. */
fun Context.shareText(text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    try {
        startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // Sem nenhum app para compartilhar.
    }
}

/**
 * Compartilha um link (e não um texto solto): o app de destino mostra a
 * prévia da página e quem recebe abre direto. [title] aparece na folha de
 * compartilhar do Android.
 */
fun Context.shareLink(url: String, title: String) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, url)
        .putExtra(Intent.EXTRA_TITLE, title)
    try {
        startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // Sem nenhum app para compartilhar.
    }
}
