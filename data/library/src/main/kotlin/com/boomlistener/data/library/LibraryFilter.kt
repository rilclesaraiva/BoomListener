// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.library

/**
 * O que entra na biblioteca (Ajustes → Biblioteca).
 *
 * [minDurationSeconds]: áudios mais curtos ficam de fora (0 = todos).
 * [onlyChosenFolders]: false = todas as músicas do aparelho; true = só as que
 * estão em [folders] (ou em subpastas delas).
 */
data class LibraryFilter(
    val minDurationSeconds: Int = 0,
    val onlyChosenFolders: Boolean = false,
    val folders: Set<String> = emptySet(),
) {
    companion object {
        /** Fim da barra deslizante. */
        const val MAX_MIN_DURATION_SECONDS = 120
    }
}

/**
 * Uma pasta do aparelho que tem música.
 * [path] é o caminho completo (a chave salva no filtro); [name] o nome da
 * pasta; [location] o caminho sem o começo do armazenamento ("Music/Rock").
 */
data class MusicFolder(
    val path: String,
    val name: String,
    val location: String,
    val songs: Int,
)

/** Se uma música com essa duração, nessa pasta, entra na biblioteca. */
fun LibraryFilter.accepts(durationMs: Long, folder: String?): Boolean {
    // Duração desconhecida (0) não é escondida: o arquivo pode só não ter sido lido ainda.
    if (minDurationSeconds > 0 && durationMs > 0 && durationMs < minDurationSeconds * 1000L) return false
    if (!onlyChosenFolders) return true
    val path = folder ?: return false
    return folders.any { chosen -> path == chosen || path.startsWith("$chosen/") }
}

/**
 * A pasta de um arquivo. Usa o caminho completo quando o sistema informa;
 * senão o caminho relativo do MediaStore ("Music/Rock/").
 */
internal fun parentFolder(dataPath: String?, relativePath: String?): String? {
    dataPath?.takeIf { '/' in it }?.let { return it.substringBeforeLast('/').ifEmpty { null } }
    return relativePath?.trim()?.trimEnd('/')?.takeIf { it.isNotEmpty() }
}

private val InternalStorage = Regex("^/storage/emulated/\\d+/?")
private val ExternalVolume = Regex("^/storage/([^/]+)/?")

/** "/storage/emulated/0/Music/Rock" → "Music/Rock"; cartão SD → "1234-5678/Music". */
internal fun folderLocation(path: String): String {
    InternalStorage.find(path)?.let { return path.substring(it.range.last + 1).ifEmpty { "/" } }
    ExternalVolume.find(path)?.let { match ->
        val rest = path.substring(match.range.last + 1)
        return if (rest.isEmpty()) match.groupValues[1] else "${match.groupValues[1]}/$rest"
    }
    return path
}

/** Agrupa as pastas das músicas, em ordem do caminho. */
internal fun groupFolders(folderOfEachSong: List<String?>): List<MusicFolder> =
    folderOfEachSong.filterNotNull()
        .groupingBy { it }
        .eachCount()
        .map { (path, count) ->
            val location = folderLocation(path)
            MusicFolder(
                path = path,
                name = location.substringAfterLast('/').ifEmpty { location },
                location = location,
                songs = count,
            )
        }
        .sortedBy { it.location.lowercase() }
