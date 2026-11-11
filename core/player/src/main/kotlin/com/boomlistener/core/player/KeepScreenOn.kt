// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.player

/**
 * Onde a pessoa pode pedir "manter a tela ativa" (Ajustes → Reprodução).
 * [Player] vale para o player em tela cheia inteiro (capa e letra);
 * [Lyrics] só quando a letra está na tela; [Everywhere] em qualquer tela do app.
 */
enum class ScreenOnPlace { Player, Lyrics, Home, Library, Profile, Everywhere }

/**
 * Se a tela deve ficar acesa agora.
 *
 * [tab]: a aba aberta (Início, Biblioteca ou Perfil), ou null numa tela de dentro
 * (artista, álbum, ajustes…), que só conta em [ScreenOnPlace.Everywhere].
 * Com o player aberto ele cobre o app: só valem [ScreenOnPlace.Player],
 * [ScreenOnPlace.Lyrics] (letra na tela) e [ScreenOnPlace.Everywhere].
 */
fun shouldKeepScreenOn(
    places: Set<ScreenOnPlace>,
    onlyWhilePlaying: Boolean,
    playing: Boolean,
    tab: ScreenOnPlace?,
    playerOpen: Boolean,
    lyricsShown: Boolean,
): Boolean {
    if (places.isEmpty()) return false
    if (onlyWhilePlaying && !playing) return false
    return when {
        ScreenOnPlace.Everywhere in places -> true
        playerOpen -> ScreenOnPlace.Player in places || (lyricsShown && ScreenOnPlace.Lyrics in places)
        else -> tab != null && tab in places
    }
}
