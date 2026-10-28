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
    /** O aleatório estava ligado (a fila acima já é a ordem embaralhada). */
    val shuffle: Boolean = false,
    /** Com o aleatório ligado: a ordem original, para desligá-lo depois. */
    val original: List<SnapshotItem>? = null,
)

private const val HEADER = "boom-queue-v1"
/** Separa a fila da ordem original (linhas de música começam pelo endereço, nunca por "#"). */
private const val ORIGINAL_MARK = "#original"

/** Texto simples, uma música por linha, com tab separando os campos (puro e testável). */
internal fun encodeSnapshot(snapshot: PlaybackSnapshot): String = buildString {
    append(HEADER).append('\t').append(snapshot.index).append('\t').append(snapshot.positionMs)
        .append('\t').append(snapshot.repeatMode).append('\t').append(if (snapshot.shuffle) 1 else 0).append('\n')
    fun line(item: SnapshotItem) {
        append(listOf(item.uri, item.title, item.artist.orEmpty(), item.album.orEmpty()).joinToString("\t") { escape(it) })
        append('\n')
    }
    snapshot.items.forEach(::line)
    snapshot.original?.let { original ->
        append(ORIGINAL_MARK).append('\n')
        original.forEach(::line)
    }
}

internal fun decodeSnapshot(text: String): PlaybackSnapshot? {
    val lines = text.split('\n').filter { it.isNotEmpty() }
    val header = lines.firstOrNull()?.split('\t') ?: return null
    if (header.size < 4 || header[0] != HEADER) return null
    fun parse(line: String): SnapshotItem? {
        val fields = line.split('\t').map(::unescape)
        if (fields.size < 4 || fields[0].isEmpty()) return null
        return SnapshotItem(fields[0], fields[1], fields[2].ifEmpty { null }, fields[3].ifEmpty { null })
    }
    val body = lines.drop(1)
    val mark = body.indexOf(ORIGINAL_MARK)
    val items = (if (mark >= 0) body.subList(0, mark) else body).mapNotNull(::parse)
    val original = if (mark >= 0) body.subList(mark + 1, body.size).mapNotNull(::parse).takeIf { it.isNotEmpty() } else null
    if (items.isEmpty()) return null
    return PlaybackSnapshot(
        items = items,
        index = (header[1].toIntOrNull() ?: 0).coerceIn(items.indices),
        positionMs = (header[2].toLongOrNull() ?: 0L).coerceAtLeast(0),
        repeatMode = header[3].toIntOrNull() ?: 0,
        shuffle = header.getOrNull(4) == "1",
        original = original,
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
    /** Espera as gravações pendentes (até [timeoutMs]); usado antes de fechar o app de vez. */
    fun flush(timeoutMs: Long = 1_000) {
        runCatching { writer.submit {}.get(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS) }
    }

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
