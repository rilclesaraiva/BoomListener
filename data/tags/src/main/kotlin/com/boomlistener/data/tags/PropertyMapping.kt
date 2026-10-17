// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.tags

import com.boomlistener.core.model.TrackTags

/*
 * Conversão entre as tags do arquivo (o "mapa de propriedades" do TagLib,
 * igual para MP3, FLAC, M4A, OGG…) e o formulário do editor. Puro Kotlin,
 * para ser testado sem aparelho.
 */

internal const val KEY_TITLE = "TITLE"
internal const val KEY_ARTIST = "ARTIST"
internal const val KEY_ALBUM = "ALBUM"
internal const val KEY_ALBUM_ARTIST = "ALBUMARTIST"
internal const val KEY_DATE = "DATE"
internal const val KEY_GENRE = "GENRE"
internal const val KEY_TRACK = "TRACKNUMBER"
internal const val KEY_TRACK_TOTAL = "TRACKTOTAL"
internal const val KEY_DISC = "DISCNUMBER"
internal const val KEY_DISC_TOTAL = "DISCTOTAL"
internal const val KEY_COMPOSER = "COMPOSER"
internal const val KEY_LYRICS = "LYRICS"
private const val KEY_UNSYNCED_LYRICS = "UNSYNCEDLYRICS"

/** Separador ao juntar vários valores (vários artistas) num campo só. */
private const val MULTI_SEPARATOR = "; "

/** Lê as tags do arquivo para o formulário. */
internal fun tagsFromProperties(properties: Map<String, List<String>>): TrackTags {
    val map = properties.mapKeys { it.key.uppercase() }
    fun first(key: String) = map[key].orEmpty().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
    fun joined(key: String) = map[key].orEmpty().map { it.trim() }.filter { it.isNotEmpty() }
        .joinToString(MULTI_SEPARATOR)

    val (track, trackTotalInline) = splitNumber(first(KEY_TRACK))
    val (disc, discTotalInline) = splitNumber(first(KEY_DISC))
    return TrackTags(
        title = first(KEY_TITLE),
        artist = joined(KEY_ARTIST),
        album = first(KEY_ALBUM),
        albumArtist = joined(KEY_ALBUM_ARTIST),
        year = yearOf(first(KEY_DATE)),
        genre = joined(KEY_GENRE),
        trackNumber = track,
        trackTotal = trackTotalInline.ifEmpty { cleanNumber(first(KEY_TRACK_TOTAL)) },
        discNumber = disc,
        discTotal = discTotalInline.ifEmpty { cleanNumber(first(KEY_DISC_TOTAL)) },
        composer = joined(KEY_COMPOSER),
        lyrics = map[KEY_LYRICS].orEmpty().firstOrNull { it.isNotBlank() }
            ?: map[KEY_UNSYNCED_LYRICS].orEmpty().firstOrNull { it.isNotBlank() }
            ?: "",
    )
}

/**
 * Aplica o formulário sobre as tags originais. Tudo o que o editor não mostra
 * (comentários, códigos ISRC, ReplayGain…) é mantido como estava. Campo vazio
 * apaga a tag.
 */
internal fun applyTags(original: Map<String, List<String>>, tags: TrackTags): Map<String, List<String>> {
    val result = LinkedHashMap<String, List<String>>()
    original.forEach { (key, values) -> result[key.uppercase()] = values }

    fun set(key: String, value: String) {
        val clean = value.trim()
        if (clean.isEmpty()) result.remove(key) else result[key] = listOf(clean)
    }

    // Vários valores (ex.: dois artistas): se o texto não mudou, mantém a
    // lista original em vez de juntar tudo num valor só.
    fun setMulti(key: String, value: String) {
        val unchanged = original.entries.firstOrNull { it.key.uppercase() == key }?.value
            ?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?.joinToString(MULTI_SEPARATOR) == value.trim()
        if (!unchanged) set(key, value)
    }

    set(KEY_TITLE, tags.title)
    setMulti(KEY_ARTIST, tags.artist)
    set(KEY_ALBUM, tags.album)
    setMulti(KEY_ALBUM_ARTIST, tags.albumArtist)
    setMulti(KEY_GENRE, tags.genre)
    setMulti(KEY_COMPOSER, tags.composer)

    // Data: se o arquivo tem a data completa (2023-05-12) e o ano não mudou,
    // mantém a data completa.
    val originalDate = result[KEY_DATE]?.firstOrNull().orEmpty()
    if (yearOf(originalDate) != tags.year.trim()) set(KEY_DATE, tags.year)

    writeNumber(result, KEY_TRACK, KEY_TRACK_TOTAL, tags.trackNumber, tags.trackTotal)
    writeNumber(result, KEY_DISC, KEY_DISC_TOTAL, tags.discNumber, tags.discTotal)

    // Letra: mantém as quebras de linha; só tira espaço sobrando nas pontas.
    val lyrics = tags.lyrics.trim('\n', '\r', ' ')
    result.remove(KEY_UNSYNCED_LYRICS)
    if (lyrics.isEmpty()) result.remove(KEY_LYRICS) else result[KEY_LYRICS] = listOf(lyrics)

    return result
}

/**
 * Faixa e disco: se o arquivo guarda o total numa tag própria (FLAC/OGG),
 * continua assim; senão grava junto, no formato "3/12".
 */
private fun writeNumber(
    result: MutableMap<String, List<String>>,
    numberKey: String,
    totalKey: String,
    number: String,
    total: String,
) {
    val cleanNumber = cleanNumber(number)
    val cleanTotal = cleanNumber(total)
    when {
        cleanNumber.isEmpty() -> {
            result.remove(numberKey)
            result.remove(totalKey)
        }
        totalKey in result -> {
            result[numberKey] = listOf(cleanNumber)
            if (cleanTotal.isEmpty()) result.remove(totalKey) else result[totalKey] = listOf(cleanTotal)
        }
        cleanTotal.isEmpty() -> result[numberKey] = listOf(cleanNumber)
        else -> result[numberKey] = listOf("$cleanNumber/$cleanTotal")
    }
}

/** "3/12" → ("3", "12"); "03" → ("3", ""). */
internal fun splitNumber(raw: String): Pair<String, String> {
    val parts = raw.split('/', limit = 2)
    return cleanNumber(parts[0]) to cleanNumber(parts.getOrElse(1) { "" })
}

/** Só os dígitos, sem zeros à esquerda ("07" → "7"; "abc" → ""). */
internal fun cleanNumber(raw: String): String =
    raw.trim().takeWhile { it.isDigit() }.trimStart('0')

/** "2023-05-12" ou "2023" → "2023"; texto que não começa com ano fica como está. */
internal fun yearOf(date: String): String {
    val trimmed = date.trim()
    return if (trimmed.length >= 4 && trimmed.take(4).all { it.isDigit() }) trimmed.take(4) else trimmed
}
