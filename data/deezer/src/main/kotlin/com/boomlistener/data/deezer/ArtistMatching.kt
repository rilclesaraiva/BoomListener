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
