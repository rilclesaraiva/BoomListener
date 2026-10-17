// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.lastfm

/*
 * As tags do Last.fm são escritas pelos ouvintes: misturam gênero ("math
 * rock") com opinião ("favorites", "seen live") e época ("90s"). Aqui sai só
 * um gênero, bem escrito, para a tag do arquivo.
 */

private val NOT_A_GENRE = setOf(
    "seen live", "favorites", "favourites", "favorite", "favourite", "love", "loved", "awesome",
    "beautiful", "amazing", "best", "cool", "great", "good", "fun", "sad", "happy", "chill",
    "female vocalists", "male vocalists", "female vocalist", "male vocalist", "female vocals", "male vocals",
    "my music", "music", "songs", "albums i own", "under 2000 listeners", "spotify", "youtube",
    "british", "american", "brazilian", "brasil", "brazil", "japanese", "korean", "english", "usa", "uk",
)
private val DECADE = Regex("""^(\d{2}|\d{4})s$""")
private val YEAR = Regex("""^\d{4}$""")
private val ACRONYMS = mapOf("mpb" to "MPB", "edm" to "EDM", "idm" to "IDM", "rnb" to "R&B", "r&b" to "R&B", "uk garage" to "UK Garage")

/**
 * O primeiro gênero de verdade em [tags] (já em ordem de popularidade), ou
 * null. [artistName] fica de fora: muita gente marca a música com o nome do artista.
 */
fun pickGenre(tags: List<String>, artistName: String? = null): String? {
    val artist = artistName?.trim()?.lowercase()
    return tags.asSequence()
        .map { it.trim() }
        .filter { it.length in 2..30 }
        .filterNot { tag ->
            val lower = tag.lowercase()
            lower in NOT_A_GENRE || DECADE.matches(lower) || YEAR.matches(lower) || lower == artist
        }
        .firstOrNull()
        ?.let(::prettyGenre)
}

/** "math rock" → "Math Rock"; "j-pop" → "J-Pop"; "mpb" → "MPB". */
fun prettyGenre(tag: String): String {
    val lower = tag.trim().lowercase()
    ACRONYMS[lower]?.let { return it }
    return lower.split(' ').joinToString(" ") { word ->
        word.split('-').joinToString("-") { part -> part.replaceFirstChar { it.uppercase() } }
    }
}
