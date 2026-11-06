// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.tags

import com.boomlistener.core.model.TrackTags

/** O que pode faltar numa música (o escaneamento do editor de tags procura isto). */
enum class TagGap { Artist, Album, Year, Genre, TrackNumber, AlbumArtist, Composer, Cover, Lyrics }

/**
 * O que falta nas [tags] lidas do arquivo, na ordem de [TagGap]. [hasCover] diz
 * se o arquivo tem capa embutida. Puro, para ser testado sem aparelho.
 */
fun gapsOf(tags: TrackTags, hasCover: Boolean): Set<TagGap> = buildSet {
    if (tags.artist.isBlank()) add(TagGap.Artist)
    if (tags.album.isBlank()) add(TagGap.Album)
    if (tags.year.isBlank()) add(TagGap.Year)
    if (tags.genre.isBlank()) add(TagGap.Genre)
    if (tags.trackNumber.isBlank()) add(TagGap.TrackNumber)
    if (tags.albumArtist.isBlank()) add(TagGap.AlbumArtist)
    if (tags.composer.isBlank()) add(TagGap.Composer)
    if (!hasCover) add(TagGap.Cover)
    if (tags.lyrics.isBlank()) add(TagGap.Lyrics)
}
