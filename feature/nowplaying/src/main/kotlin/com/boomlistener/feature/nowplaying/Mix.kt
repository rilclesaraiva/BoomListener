// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import com.boomlistener.core.model.Track
import kotlin.random.Random

/** Quantas músicas parecidas (mesmo artista ou gênero) abrem o Mix. */
internal const val MIX_RELATED_FIRST = 5

/**
 * O "Mix" do player: depois da música [currentUri], o resto da biblioteca
 * misturado. As primeiras são parecidas com ela (mesmo artista ou gênero,
 * até [MIX_RELATED_FIRST]), para a passagem não ser brusca; depois vem
 * tudo, cada música uma vez só. A própria música atual não entra.
 */
internal fun buildMix(currentUri: String?, library: List<Track>, random: Random = Random.Default): List<Track> {
    val current = library.firstOrNull { it.uri == currentUri }
    val others = library.filter { it.uri != currentUri }.distinctBy { it.uri }.shuffled(random)
    if (current == null) return others
    fun String?.norm() = this?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
    val artist = current.artist.norm()
    val genre = current.genre.norm()
    val related = others.filter { (artist != null && it.artist.norm() == artist) || (genre != null && it.genre.norm() == genre) }
        .take(MIX_RELATED_FIRST)
    val chosen = related.map { it.uri }.toSet()
    return related + others.filter { it.uri !in chosen }
}
