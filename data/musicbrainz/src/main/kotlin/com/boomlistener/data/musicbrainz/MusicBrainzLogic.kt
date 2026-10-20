// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.musicbrainz

import com.boomlistener.core.common.normalizedForSearch
import com.boomlistener.core.model.ReleaseType
import com.boomlistener.core.model.TagSuggestion
import com.boomlistener.core.model.TrackTags
import java.util.UUID

/** Os ids do MusicBrainz são UUIDs; as telas usam Long. Negativo, para nunca bater com um id do Deezer. */
fun musicBrainzKey(mbid: String): Long =
    runCatching { UUID.fromString(mbid).mostSignificantBits }.getOrElse { mbid.hashCode().toLong() }
        .let { if (it > 0) -it else if (it == 0L) -1L else it }

/** Aspas e barras escapadas para a busca do MusicBrainz (Lucene). */
internal fun luceneQuote(text: String): String =
    "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

/** Tipo do grupo de lançamento do MusicBrainz → o nosso. null = não entra na discografia (ao vivo, trilha…). */
internal fun releaseTypeOf(primary: String?, secondary: List<String>): ReleaseType? {
    val extras = secondary.map { it.lowercase() }
    if (extras.any { it in SKIPPED_SECONDARY }) return null
    if ("compilation" in extras) return ReleaseType.Compilation
    return when (primary?.lowercase()) {
        "album" -> ReleaseType.Album
        "ep" -> ReleaseType.EP
        "single" -> ReleaseType.Single
        else -> null
    }
}

private val SKIPPED_SECONDARY = setOf("live", "demo", "interview", "spokenword", "audiobook", "dj-mix", "mixtape/street", "remix", "soundtrack", "field recording", "audio drama")

/** Um lançamento em que a gravação aparece (o que a busca de gravações devolve). */
internal data class MbReleaseOption(
    val id: String,
    val groupId: String?,
    val title: String,
    val date: String,
    val official: Boolean,
    val primaryType: String?,
    val secondaryTypes: List<String>,
    val trackNumber: String,
    val trackTotal: Int,
    val discNumber: Int,
    val discTotal: Int,
)

/**
 * Entre os lançamentos de uma gravação, o "original": oficial, álbum (depois
 * EP, depois single), sem coletânea nem ao vivo, e o mais antigo.
 */
internal fun bestRelease(options: List<MbReleaseOption>): MbReleaseOption? =
    options.minWithOrNull(
        compareBy<MbReleaseOption>(
            { if (it.official) 0 else 1 },
            { if (it.secondaryTypes.isEmpty()) 0 else 1 },
            {
                when (it.primaryType?.lowercase()) {
                    "album" -> 0
                    "ep" -> 1
                    "single" -> 2
                    else -> 3
                }
            },
            { it.date.ifBlank { "9999" } },
        ),
    )

/** Um artista achado na busca do MusicBrainz. */
internal data class MbArtistHit(val id: String, val name: String, val score: Int, val aliases: List<String>)

/**
 * Os artistas que podem ser [tagName]: nome (ou apelido) igual, sem acento e
 * maiúscula, do mais provável para o menos. "Fulano feat. Ciclano" vale como "Fulano".
 */
internal fun matchingArtists(tagName: String, hits: List<MbArtistHit>): List<MbArtistHit> {
    val wanted = primaryName(tagName).normalizedForSearch()
    if (wanted.isEmpty()) return emptyList()
    return hits
        .filter { hit -> (listOf(hit.name) + hit.aliases).any { it.normalizedForSearch() == wanted } }
        .sortedByDescending { it.score }
}

private val FEATURING = Regex("""\s*[(\[]?\s*(feat\.?|ft\.?|featuring)\s.*$""", RegexOption.IGNORE_CASE)

internal fun primaryName(tagName: String): String = tagName.replace(FEATURING, "").trim()

private val NOT_WORD = Regex("""[^\p{L}\p{N}]+""")

private fun comparableAlbum(title: String): String =
    title.replace(Regex("""\s*[(\[][^)\]]*[)\]]"""), "").normalizedForSearch().replace(NOT_WORD, " ").trim()

/** Quantos álbuns do aparelho aparecem na discografia (para escolher entre homônimos). */
internal fun albumsInCommon(localAlbums: List<String>, discography: List<String>): Int {
    val remote = discography.map(::comparableAlbum).filter { it.isNotEmpty() }.toSet()
    return localAlbums.map(::comparableAlbum).filter { it.isNotEmpty() }.distinct().count { it in remote }
}

/** A gravação como sugestão de tags (o gênero fica para o Last.fm). */
fun MbRecording.toTagSuggestion(confidence: Float): TagSuggestion = TagSuggestion(
    tags = TrackTags(
        title = title,
        artist = artist,
        album = album,
        albumArtist = artist,
        year = date.take(4).takeIf { it.length == 4 && it.all(Char::isDigit) }.orEmpty(),
        trackNumber = trackNumber,
        trackTotal = trackTotal,
        discNumber = discNumber,
        discTotal = discTotal,
    ),
    coverUrl = releaseId?.let { "$COVER_ART_URL/release/$it/front" },
    durationMs = durationMs,
    confidence = confidence,
    source = "MusicBrainz",
    link = link,
)
