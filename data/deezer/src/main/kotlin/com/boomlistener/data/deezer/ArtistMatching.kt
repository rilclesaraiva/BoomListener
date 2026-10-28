// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.deezer

import com.boomlistener.core.common.normalizedForSearch
import com.boomlistener.core.model.ExternalArtist

private val FEATURING = Regex("""\s+(feat\.?|ft\.?|featuring|part\.?)\s+.*$""", RegexOption.IGNORE_CASE)

/**
 * O artista principal de um nome de tag: "Djavan feat. Caetano" → "Djavan".
 * Não divide por "&" ou "," porque muitos nomes de banda têm isso
 * ("Simon & Garfunkel", "Earth, Wind & Fire").
 */
fun primaryArtistName(tagName: String): String = tagName.replace(FEATURING, "").trim()

// Separadores entre artistas: vírgula, ";", "/", "&", "+", "x", "×", "y", "e",
// "and", "with", "vs", "feat.", "ft.", "featuring".
private val ARTIST_SEPARATOR = Regex(
    """\s*[,;/]\s*|\s+(?:&|\+|x|×|y|e|and|with|vs\.?|feat\.?|ft\.?|featuring)\s+""",
    RegexOption.IGNORE_CASE,
)

/**
 * Cada artista de um nome de tag com vários: "Salako, Ziahfyah y ELHAE" →
 * [Salako, Ziahfyah, ELHAE]; "Djavan feat. Caetano" → [Djavan, Caetano].
 * Um nome só volta sozinho. Quebra também nomes de banda ("Earth, Wind &
 * Fire"), por isso quem procura tenta o nome inteiro antes ([artistSearchNames]).
 */
fun splitArtistNames(tagName: String): List<String> =
    tagName.split(ARTIST_SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }

/**
 * Nomes para procurar o artista, na ordem: o nome inteiro (sem o "feat."),
 * que acerta bandas com "&" ou vírgula, e depois cada artista separado.
 */
fun artistSearchNames(tagName: String, limit: Int = 4): List<String> =
    (listOf(primaryArtistName(tagName)) + splitArtistNames(tagName))
        .filter { it.isNotBlank() }
        .distinctBy { it.normalizedForSearch() }
        .take(limit)

/**
 * Escolhe, entre os resultados da busca, o artista certo para o nome da tag:
 * nome igual (ignorando acento e maiúscula) primeiro; entre homônimos, o com
 * mais fãs. Se nenhum nome bate, desiste (melhor não mostrar do que mostrar o
 * artista errado).
 */
fun pickArtist(tagName: String, candidates: List<ExternalArtist>): ExternalArtist? {
    val wanted = primaryArtistName(tagName).normalizedForSearch()
    return candidates
        .filter { it.name.normalizedForSearch() == wanted }
        .maxByOrNull { it.fans }
}

private val EDITION = Regex("""\s*[(\[][^)\]]*(edition|deluxe|remaster|version|expanded|anniversary|bonus)[^)\]]*[)\]]""", RegexOption.IGNORE_CASE)
private val SUFFIX = Regex("""\s+-\s+(single|ep)$""", RegexOption.IGNORE_CASE)

/**
 * Nome de álbum comparável: sem "(Deluxe Edition)", "[Remastered 2011]",
 * "- Single", "- EP", acentos e maiúsculas.
 */
fun comparableTitle(title: String): String =
    title.replace(EDITION, "").replace(SUFFIX, "").normalizedForSearch()

/**
 * O campo "Artista" com todos os da música: os principais separados por
 * vírgula e os convidados depois de "feat." ("Salako, Ziahfyah, ELHAE";
 * "Djavan feat. Caetano Veloso"). Se o título já cita o convidado ("(feat.
 * Fulano)"), ele não se repete no artista.
 */
fun joinArtists(main: List<String>, featured: List<String>, title: String): String {
    val mainPart = main.filter { it.isNotBlank() }.distinct().joinToString(", ")
    val inTitle = title.normalizedForSearch()
    val guests = featured.filter { it.isNotBlank() && !inTitle.contains(it.normalizedForSearch()) }.distinct()
    return if (guests.isEmpty() || mainPart.isEmpty()) mainPart.ifEmpty { guests.joinToString(", ") }
    else "$mainPart feat. ${guests.joinToString(", ")}"
}
