// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.library

import com.boomlistener.core.common.normalizedForSearch
import com.boomlistener.core.model.Track
import kotlin.math.abs

/** Diferença máxima de duração (ms) para duas faixas ainda contarem como a mesma música. */
const val DUPLICATE_DURATION_TOLERANCE_MS = 3_000L

/** Músicas que parecem ser a mesma pela coerência das tags (só sugestão: nada é apagado sozinho). */
data class DuplicateGroup(val tracks: List<Track>)

private val FEAT_IN_BRACKETS = Regex("""[(\[]\s*(?:feat\.?|ft\.?|featuring)\s[^)\]]*[)\]]""")
private val FEAT_TRAILING = Regex("""\s+(?:-\s*)?(?:feat\.?|ft\.?|featuring)\s.*$""")
private val NOT_LETTER_OR_DIGIT = Regex("""[^\p{L}\p{N}]+""")

private fun String.duplicateKeyPart(): String =
    normalizedForSearch()
        .replace(FEAT_IN_BRACKETS, " ")
        .replace(FEAT_TRAILING, "")
        .replace(NOT_LETTER_OR_DIGIT, " ")
        .trim()

/**
 * Procura músicas repetidas só pelas tags: mesmo título e mesmo artista (sem acento,
 * maiúscula, pontuação nem "feat. …") e duração parecida (até
 * [DUPLICATE_DURATION_TOLERANCE_MS]). Versões diferentes ("Live", "Remix", "Acoustic")
 * ficam de fora porque a marca faz parte do título; homônimos de artistas diferentes
 * também. Faixas sem artista ou sem duração nunca entram, porque não há com o que
 * confirmar. O que difere no resto das tags (álbum, ano) não impede o agrupamento:
 * a mesma música num álbum e numa coletânea é repetida.
 */
fun findDuplicateGroups(tracks: List<Track>): List<DuplicateGroup> =
    tracks
        .filter { it.durationMs > 0 && !it.artist.isNullOrBlank() }
        .groupBy { it.title.duplicateKeyPart() to it.artist.orEmpty().duplicateKeyPart() }
        .filterKeys { (title, artist) -> title.isNotEmpty() && artist.isNotEmpty() }
        .values
        .flatMap { sameName -> clusterByDuration(sameName) }
        .filter { it.size > 1 }
        .map { DuplicateGroup(it) }
        .sortedBy { it.tracks.first().title.normalizedForSearch() }

/** Separa em grupos cujas durações vão encadeando dentro da tolerância (do mais curto ao mais longo). */
private fun clusterByDuration(tracks: List<Track>): List<List<Track>> {
    val result = mutableListOf<MutableList<Track>>()
    for (track in tracks.sortedBy { it.durationMs }) {
        val current = result.lastOrNull()
        if (current != null && abs(track.durationMs - current.last().durationMs) <= DUPLICATE_DURATION_TOLERANCE_MS) {
            current += track
        } else {
            result += mutableListOf(track)
        }
    }
    return result
}
