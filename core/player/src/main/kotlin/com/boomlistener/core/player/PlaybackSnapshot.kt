// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.player

import android.content.Context
import java.io.File
import java.util.concurrent.Executors

/** Uma música da fila guardada (o suficiente para remontar o item do player). */
internal data class SnapshotItem(val uri: String, val title: String, val artist: String?, val album: String?)

/**
 * Onde o player estava: a fila, qual música e em que ponto. Guardado para,
 * ao abrir o app de novo, o mini player voltar no mesmo lugar (pausado).
 * Encerrar a música (puxar o mini player para baixo) apaga isto.
 */
internal data class PlaybackSnapshot(
    val items: List<SnapshotItem>,
    val index: Int,
    val positionMs: Long,
    val repeatMode: Int,
)

private const val HEADER = "boom-queue-v1"

/** Texto simples, uma música por linha, com tab separando os campos (puro e testável). */
internal fun encodeSnapshot(snapshot: PlaybackSnapshot): String = buildString {
    append(HEADER).append('\t').append(snapshot.index).append('\t').append(snapshot.positionMs)
        .append('\t').append(snapshot.repeatMode).append('\n')
    snapshot.items.forEach { item ->
        append(listOf(item.uri, item.title, item.artist.orEmpty(), item.album.orEmpty()).joinToString("\t") { escape(it) })
        append('\n')
    }
}

internal fun decodeSnapshot(text: String): PlaybackSnapshot? {
    val lines = text.split('\n').filter { it.isNotEmpty() }
    val header = lines.firstOrNull()?.split('\t') ?: return null
    if (header.size < 4 || header[0] != HEADER) return null
    val items = lines.drop(1).mapNotNull { line ->
        val fields = line.split('\t').map(::unescape)
        if (fields.size < 4 || fields[0].isEmpty()) return@mapNotNull null
        SnapshotItem(fields[0], fields[1], fields[2].ifEmpty { null }, fields[3].ifEmpty { null })
    }
    if (items.isEmpty()) return null
    return PlaybackSnapshot(
        items = items,
        index = (header[1].toIntOrNull() ?: 0).coerceIn(items.indices),
        positionMs = (header[2].toLongOrNull() ?: 0L).coerceAtLeast(0),
        repeatMode = header[3].toIntOrNull() ?: 0,
    )
}

private fun escape(value: String): String =
    value.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n")

private fun unescape(value: String): String {
    val out = StringBuilder(value.length)
    var i = 0
    while (i < value.length) {
        val c = value[i]
        if (c == '\\' && i + 1 < value.length) {
            when (value[i + 1]) {
                't' -> out.append('\t')
                'n' -> out.append('\n')
                else -> out.append(value[i + 1])
            }
            i += 2
        } else {
            out.append(c)
            i++
        }
    }
    return out.toString()
}

/** Guarda e lê a fila num arquivo do app. A gravação roda fora da thread principal. */
internal class PlaybackSnapshotStore(context: Context) {
    private val file = File(context.filesDir, "player/last_queue.txt")
    private val writer = Executors.newSingleThreadExecutor()

    fun read(): PlaybackSnapshot? = runCatching { if (file.exists()) decodeSnapshot(file.readText()) else null }.getOrNull()

    /** null apaga (fila vazia: o usuário encerrou a música). */
    fun save(snapshot: PlaybackSnapshot?) {
        val text = snapshot?.let(::encodeSnapshot)
        writer.execute {
            runCatching {
                if (text == null) {
                    file.delete()
                } else {
                    file.parentFile?.mkdirs()
                    val temp = File(file.parentFile, file.name + ".tmp")
                    temp.writeText(text)
                    temp.renameTo(file)
                }
            }
        }
    }
}
