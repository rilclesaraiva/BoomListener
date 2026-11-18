// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.profile

import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.Inflater

/** Uma entrada do índice (diretório central) de um .zip. */
internal class ZipItem(
    val name: String,
    val method: Int,
    val flags: Int,
    val compressedSize: Long,
    val size: Long,
    val headerOffset: Long,
) {
    val isDirectory: Boolean get() = name.endsWith("/")
}

/**
 * Leitor mínimo de .zip pelo índice, só para este app. Existe porque o `ZipFile` e o `ZipInputStream`
 * do Android 14+ recusam o arquivo inteiro se uma entrada tiver nome como "/" ou com "..", e os
 * pacotes de shimeji trazem isso; aqui os nomes são só lidos (quem usa decide o que aceitar) e
 * nada é gravado a partir deles. Sem zip64 nem senha.
 */
internal class ZipArchive(file: File) : Closeable {
    private val raf = RandomAccessFile(file, "r")
    val entries: List<ZipItem>

    init {
        try {
            entries = readIndex()
        } catch (e: Throwable) {
            raf.close()
            throw e
        }
    }

    private fun readIndex(): List<ZipItem> {
        val length = raf.length()
        val tailSize = minOf(length, 65_557L).toInt()
        val tail = ByteArray(tailSize)
        raf.seek(length - tailSize)
        raf.readFully(tail)
        var eocd = -1
        for (i in tailSize - 22 downTo 0) {
            if (tail[i] == 0x50.toByte() && tail[i + 1] == 0x4B.toByte() && tail[i + 2] == 0x05.toByte() && tail[i + 3] == 0x06.toByte()) {
                eocd = i
                break
            }
        }
        require(eocd >= 0) { "sem fim de zip" }
        val end = ByteBuffer.wrap(tail, eocd, 22).order(ByteOrder.LITTLE_ENDIAN)
        val count = end.getShort(eocd + 10).toInt() and 0xFFFF
        val cdSize = end.getInt(eocd + 12).toLong() and 0xFFFFFFFFL
        val cdOffset = end.getInt(eocd + 16).toLong() and 0xFFFFFFFFL
        require(cdSize in 0..MAX_INDEX_BYTES && cdOffset + cdSize <= length) { "índice inválido" }
        val index = ByteArray(cdSize.toInt())
        raf.seek(cdOffset)
        raf.readFully(index)
        val buf = ByteBuffer.wrap(index).order(ByteOrder.LITTLE_ENDIAN)
        val list = ArrayList<ZipItem>()
        var pos = 0
        repeat(count) {
            if (pos + 46 > index.size || buf.getInt(pos) != 0x02014b50) return list
            val flags = buf.getShort(pos + 8).toInt() and 0xFFFF
            val method = buf.getShort(pos + 10).toInt() and 0xFFFF
            val compressed = buf.getInt(pos + 20).toLong() and 0xFFFFFFFFL
            val size = buf.getInt(pos + 24).toLong() and 0xFFFFFFFFL
            val nameLen = buf.getShort(pos + 28).toInt() and 0xFFFF
            val extraLen = buf.getShort(pos + 30).toInt() and 0xFFFF
            val commentLen = buf.getShort(pos + 32).toInt() and 0xFFFF
            val offset = buf.getInt(pos + 42).toLong() and 0xFFFFFFFFL
            if (pos + 46 + nameLen > index.size) return list
            list += ZipItem(String(index, pos + 46, nameLen, Charsets.UTF_8), method, flags, compressed, size, offset)
            pos += 46 + nameLen + extraLen + commentLen
        }
        return list
    }

    /** O conteúdo de [item]; null se passar de [limit] bytes ou se o formato não for suportado. */
    fun read(item: ZipItem, limit: Int): ByteArray? {
        if (item.isDirectory || item.flags and 1 != 0) return null
        if (item.size > limit || item.compressedSize > limit) return null
        val header = ByteArray(30)
        raf.seek(item.headerOffset)
        raf.readFully(header)
        val h = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        if (h.getInt(0) != 0x04034b50) return null
        val start = item.headerOffset + 30 + (h.getShort(26).toInt() and 0xFFFF) + (h.getShort(28).toInt() and 0xFFFF)
        if (start + item.compressedSize > raf.length()) return null
        val packed = ByteArray(item.compressedSize.toInt())
        raf.seek(start)
        raf.readFully(packed)
        return when (item.method) {
            0 -> packed
            8 -> inflate(packed, limit)
            else -> null
        }
    }

    private fun inflate(packed: ByteArray, limit: Int): ByteArray? {
        val inflater = Inflater(true)
        try {
            inflater.setInput(packed)
            val out = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            while (!inflater.finished()) {
                val n = inflater.inflate(buffer)
                if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) break
                out.write(buffer, 0, n)
                if (out.size() > limit) return null
            }
            return out.toByteArray()
        } catch (e: java.util.zip.DataFormatException) {
            return null
        } finally {
            inflater.end()
        }
    }

    override fun close() = raf.close()

    private companion object {
        const val MAX_INDEX_BYTES = 8L * 1024 * 1024
    }
}
