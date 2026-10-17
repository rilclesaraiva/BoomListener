// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.model

/**
 * As tags editáveis de uma música, como texto (vazio = sem a tag). Ficam como
 * texto porque é assim que o usuário digita; quem grava cuida de converter.
 *
 * [trackTotal] e [discTotal] não aparecem no formulário, mas são mantidos (e
 * preenchidos pelas tags automáticas) para gravar "3/12" em vez de só "3".
 */
data class TrackTags(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val albumArtist: String = "",
    val year: String = "",
    val genre: String = "",
    val trackNumber: String = "",
    val trackTotal: String = "",
    val discNumber: String = "",
    val discTotal: String = "",
    val composer: String = "",
    val lyrics: String = "",
)

/**
 * Sugestão de tags vinda de uma fonte oficial (Deezer, com gênero do Last.fm),
 * para a correção automática. [coverUrl] é a capa em alta resolução.
 */
data class TagSuggestion(
    val tags: TrackTags,
    val coverUrl: String?,
    val durationMs: Long,
    /** De 0 a 1: o quanto bate com o que está no arquivo. */
    val confidence: Float,
    val source: String,
    val link: String?,
)
