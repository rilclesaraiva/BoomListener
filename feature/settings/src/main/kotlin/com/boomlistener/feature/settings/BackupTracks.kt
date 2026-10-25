// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.settings

import java.text.Normalizer
import kotlin.math.abs

/*
 * As músicas são guardadas pelo endereço do MediaStore (content://media/…/ID),
 * e esse número muda de um celular para outro (e até ao reinstalar o
 * sistema). Por isso o backup leva junto um "documento" de cada música citada
 * (nome do arquivo, pasta, título, artista, álbum, duração) e, ao restaurar,
 * cada endereço antigo é trocado pelo da mesma música no aparelho de agora.
 * Tudo aqui é puro e testado.
 */

/** O que identifica uma música fora do número do MediaStore. */
internal data class BackupTrack(
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val fileName: String,
    val folder: String,
)

/** Endereços de música do MediaStore dentro de um texto (JSON com "/" escapado ou não). */
private val MEDIA_URI = Regex("""content:(?:\\?/){2}media(?:\\?/[A-Za-z0-9_.\-]+)+""")

/** Os endereços de música citados em [text], já sem o escape do JSON. */
internal fun mediaUrisIn(text: String): Set<String> =
    MEDIA_URI.findAll(text).map { it.value.replace("\\/", "/") }.toSet()

/** Troca cada endereço de [text] pelo de [map] (os que não estão no mapa ficam como estão). */
internal fun remapMediaUris(text: String, map: Map<String, String>): String =
    MEDIA_URI.replace(text) { match ->
        val escaped = match.value.contains("\\/")
        val plain = match.value.replace("\\/", "/")
        val target = map[plain] ?: return@replace match.value
        if (escaped) target.replace("/", "\\/") else target
    }

/**
 * Para cada música do backup, a mesma música entre as de agora (endereço
 * antigo → novo). Tenta, em ordem: o mesmo endereço com o mesmo arquivo
 * (mesmo aparelho); a mesma pasta e arquivo; o mesmo arquivo com a mesma
 * duração; o mesmo título e artista (e álbum, quando os dois têm) com a mesma
 * duração. Sem nenhum, a música fica de fora do mapa.
 */
internal fun matchBackupTracks(saved: List<BackupTrack>, current: List<BackupTrack>): Map<String, String> {
    val byUri = current.associateBy { it.uri }
    val byPath = current.groupBy { norm(it.folder) + "/" + norm(it.fileName) }
    val byFile = current.groupBy { norm(it.fileName) }
    val byTitle = current.groupBy { norm(it.title) + "|" + norm(it.artist) }
    val result = HashMap<String, String>()
    for (old in saved) {
        val same = byUri[old.uri]
        val match = when {
            same != null && norm(same.fileName) == norm(old.fileName) -> same
            else -> byPath[norm(old.folder) + "/" + norm(old.fileName)]?.firstOrNull { closeDuration(it, old) }
                ?: byFile[norm(old.fileName)]?.firstOrNull { closeDuration(it, old) && old.fileName.isNotBlank() }
                ?: byTitle[norm(old.title) + "|" + norm(old.artist)]?.firstOrNull { candidate ->
                    old.title.isNotBlank() && closeDuration(candidate, old, TITLE_TOLERANCE_MS) &&
                        (old.album.isBlank() || candidate.album.isBlank() || norm(old.album) == norm(candidate.album))
                }
        }
        if (match != null) result[old.uri] = match.uri
    }
    return result
}

private fun closeDuration(a: BackupTrack, b: BackupTrack, tolerance: Long = FILE_TOLERANCE_MS): Boolean =
    a.durationMs <= 0 || b.durationMs <= 0 || abs(a.durationMs - b.durationMs) <= tolerance

/** Minúsculas, sem acentos e com espaços simples (como as pessoas escrevem nomes de jeitos diferentes). */
private fun norm(text: String): String =
    Normalizer.normalize(text.trim().lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace(Regex("\\s+"), " ")

private const val FILE_TOLERANCE_MS = 2_000L
private const val TITLE_TOLERANCE_MS = 3_000L
