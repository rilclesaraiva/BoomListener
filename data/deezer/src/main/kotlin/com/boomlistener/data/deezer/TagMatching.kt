// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.deezer

import com.boomlistener.core.common.normalizedForSearch
import com.boomlistener.core.model.Release
import kotlin.math.abs

/*
 * Tags automáticas: decidir qual resultado do Deezer é a música do arquivo.
 * Puro Kotlin e testado, porque errar aqui grava a tag errada no arquivo.
 */

/** Uma música encontrada no Deezer que pode ser a do arquivo. */
data class TrackCandidate(
    val trackId: Long,
    val albumId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val coverUrl: String?,
    val durationMs: Long,
    val link: String?,
    /** De 0 a 1: o quanto bate com o que está no arquivo. */
    val confidence: Float = 0f,
)

/** O que já se sabe da música pelo arquivo (pode estar errado ou faltando). */
data class LocalTrackInfo(
    val title: String,
    val artist: String?,
    val album: String?,
    val durationMs: Long,
)

private val FEAT_IN_TITLE = Regex("""\s*[(\[]\s*(feat\.?|ft\.?|featuring|with|part\.?)\s[^)\]]*[)\]]""", RegexOption.IGNORE_CASE)
private val EXTRA_IN_TITLE = Regex("""\s*[(\[][^)\]]*(official|audio|video|lyric|clipe|visualizer|hq|hd)[^)\]]*[)\]]""", RegexOption.IGNORE_CASE)
private val TRACK_NUMBER_PREFIX = Regex("""^\d{1,3}\s*[-._)]\s*""")
private val NON_WORD = Regex("""[^\p{L}\p{N}]+""")

/**
 * Título comparável: sem "(feat. Fulano)", "[Official Video]", número de
 * faixa no começo ("03 - "), acentos, maiúsculas e pontuação.
 */
fun comparableSongTitle(title: String): String =
    title.replace(FEAT_IN_TITLE, "")
        .replace(EXTRA_IN_TITLE, "")
        .replace(TRACK_NUMBER_PREFIX, "")
        .normalizedForSearch()
        .replace(NON_WORD, " ")
        .trim()

/** Título para mandar na busca (sem os enfeites, mas com acentos). */
fun searchableSongTitle(title: String): String =
    title.replace(FEAT_IN_TITLE, "").replace(EXTRA_IN_TITLE, "").replace(TRACK_NUMBER_PREFIX, "").trim()

private fun words(text: String): Set<String> = text.split(' ').filter { it.isNotBlank() }.toSet()

/** 1 = igual; 0 = nada a ver. */
internal fun titleSimilarity(local: String, remote: String): Float {
    val a = comparableSongTitle(local)
    val b = comparableSongTitle(remote)
    if (a.isEmpty() || b.isEmpty()) return 0f
    if (a == b) return 1f
    // "Açaí" x "Açaí (Ao Vivo)": uma contém a outra.
    val remoteWithoutVersion = comparableSongTitle(comparableTitle(remote))
    if (a == remoteWithoutVersion) return 0.9f
    if (b.startsWith(a) || a.startsWith(b)) return 0.75f
    val wa = words(a)
    val wb = words(b)
    val common = wa.intersect(wb).size.toFloat()
    return common / (wa.size + wb.size - common)
}

internal fun artistSimilarity(local: String?, remote: String): Float {
    if (local.isNullOrBlank()) return 0.5f // sem artista no arquivo: não dá para conferir
    val a = primaryArtistName(local).normalizedForSearch()
    val b = primaryArtistName(remote).normalizedForSearch()
    return when {
        a == b -> 1f
        // "Anitta & J Balvin" x "Anitta"; "Djavan" x "Djavan e Caetano".
        a.startsWith(b) || b.startsWith(a) -> 0.7f
        else -> 0f
    }
}

internal fun durationSimilarity(localMs: Long, remoteMs: Long): Float {
    if (localMs <= 0 || remoteMs <= 0) return 0.5f
    val gap = abs(localMs - remoteMs)
    return when {
        gap <= 3_000 -> 1f
        gap <= 8_000 -> 0.6f
        gap <= 20_000 -> 0.25f
        else -> 0f // outra versão (ao vivo, remix, edit de rádio…)
    }
}

/** Confiança de 0 a 1 de que [candidate] é a música do arquivo. */
fun matchConfidence(local: LocalTrackInfo, candidate: TrackCandidate): Float {
    val title = titleSimilarity(local.title, candidate.title)
    val artist = artistSimilarity(local.artist, candidate.artist)
    val duration = durationSimilarity(local.durationMs, candidate.durationMs)
    val albumBonus = if (!local.album.isNullOrBlank() &&
        comparableTitle(local.album) == comparableTitle(candidate.album)
    ) 0.05f else 0f
    // Artista diferente quase nunca é a mesma música (homônimos de título).
    if (!local.artist.isNullOrBlank() && artist == 0f) return (0.2f * title).coerceAtMost(0.2f)
    // Título com quase nada em comum ("SKY.exe (Slowed)" x "MONTAGEM DEMORA
    // (Slowed)") não é a música, mesmo sendo do mesmo artista e com duração parecida.
    if (title < MIN_TITLE_SIMILARITY) return (0.3f * title).coerceAtMost(0.3f)
    return (0.45f * title + 0.35f * artist + 0.2f * duration + albumBonus).coerceIn(0f, 1f)
}

/** Abaixo disso o título é outro (só uma palavra solta em comum, como "Slowed"). */
internal const val MIN_TITLE_SIMILARITY = 0.4f

/** Confiança mínima para aparecer na lista de opções. */
const val MIN_CONFIDENCE = 0.45f

/** Confiança a partir da qual o app escolhe sozinho (o usuário ainda revisa antes de gravar). */
const val AUTO_PICK_CONFIDENCE = 0.8f

/**
 * Ordena os resultados do mais provável para o menos, sem repetir a mesma
 * música no mesmo álbum, e deixa de fora os que não batem.
 */
fun rankCandidates(local: LocalTrackInfo, candidates: List<TrackCandidate>, limit: Int = 6): List<TrackCandidate> =
    candidates
        .map { it.copy(confidence = matchConfidence(local, it)) }
        .filter { it.confidence >= MIN_CONFIDENCE }
        .sortedByDescending { it.confidence }
        .distinctBy { it.albumId to comparableSongTitle(it.title) }
        .take(limit)

/**
 * Músicas da página do artista no Deezer (as populares dele): é o mesmo
 * lugar onde a página do artista no app encontra a música. Nelas o artista
 * principal pode ser outro (remix, participação), mas a música é do artista
 * que você tem: por isso conta como dele ([artistName]) na comparação.
 */
fun rankArtistTracks(local: LocalTrackInfo, artistName: String, tracks: List<TrackCandidate>, limit: Int = 6): List<TrackCandidate> =
    rankCandidates(local, tracks.map { it.copy(artist = artistName) }, limit)

/**
 * Álbuns do artista onde vale procurar a música: o do arquivo (pelo nome) e
 * os com o nome da própria música ("SKY.exe" para "SKY.exe (Slowed)"). É
 * como a página do artista marca o que você tem.
 */
fun albumsForTrack(local: LocalTrackInfo, releases: List<Release>, limit: Int = 3): List<Release> {
    val album = local.album?.takeIf { it.isNotBlank() }?.let(::comparableTitle)
    val song = comparableSongTitle(local.title)
    return releases.filter { release ->
        val title = comparableTitle(release.title)
        val asSong = comparableSongTitle(release.title)
        (album != null && title == album) || (asSong.isNotEmpty() && (song == asSong || song.startsWith("$asSong ")))
    }.take(limit)
}
