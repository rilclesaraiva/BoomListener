// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.social

import java.io.ByteArrayOutputStream

/**
 * Gravador de GIF animado, em Kotlin puro (o Android não tem um). Usado para
 * diminuir GIF grande demais e transformar vídeo curto em GIF no perfil.
 *
 * Uma paleta só, de até 256 cores, montada por popularidade a partir de
 * alguns quadros de amostra ([buildPalette]); cada quadro é convertido para
 * ela por uma tabela pronta (rápido) e comprimido em LZW, como manda o formato.
 * Repete para sempre (extensão NETSCAPE2.0).
 */
internal class GifEncoder(
    private val width: Int,
    private val height: Int,
    private val palette: IntArray,
    /** Tempo de cada quadro, em centésimos de segundo. */
    private val delayCs: Int,
) {
    private val out = ByteArrayOutputStream()
    private val lookup = buildLookup(palette)

    init {
        require(palette.isNotEmpty() && palette.size <= 256)
        writeHeader()
    }

    val size: Int get() = out.size()

    /** Acrescenta um quadro (pixels ARGB, [width]×[height]). */
    fun addFrame(argb: IntArray) {
        require(argb.size == width * height)
        // Graphic Control Extension: tempo do quadro; sem transparência.
        out.write(0x21); out.write(0xF9); out.write(4)
        out.write(0x00)
        writeShort(delayCs.coerceAtLeast(2))
        out.write(0); out.write(0)
        // Image Descriptor: o quadro inteiro, usando a paleta global.
        out.write(0x2C)
        writeShort(0); writeShort(0); writeShort(width); writeShort(height)
        out.write(0)
        val indices = ByteArray(argb.size) { i -> lookup[rgb555(argb[i])] }
        lzw(indices)
    }

    /** Fecha o arquivo e devolve os bytes. */
    fun finish(): ByteArray {
        out.write(0x3B)
        return out.toByteArray()
    }

    private fun writeHeader() {
        out.write("GIF89a".toByteArray(Charsets.US_ASCII))
        writeShort(width); writeShort(height)
        // Paleta global com 256 entradas (as que sobram ficam pretas).
        out.write(0xF7)
        out.write(0); out.write(0)
        for (i in 0 until 256) {
            val c = palette.getOrElse(i) { 0 }
            out.write((c shr 16) and 0xFF); out.write((c shr 8) and 0xFF); out.write(c and 0xFF)
        }
        // NETSCAPE2.0: repetir para sempre.
        out.write(0x21); out.write(0xFF); out.write(11)
        out.write("NETSCAPE2.0".toByteArray(Charsets.US_ASCII))
        out.write(3); out.write(1); writeShort(0); out.write(0)
    }

    private fun writeShort(value: Int) {
        out.write(value and 0xFF)
        out.write((value shr 8) and 0xFF)
    }

    /** LZW do GIF, com códigos de 9 a 12 bits e tabela zerada quando enche. */
    private fun lzw(indices: ByteArray) {
        val minCodeSize = 8
        out.write(minCodeSize)
        val clear = 1 shl minCodeSize
        val end = clear + 1
        val blocks = BlockWriter(out)
        val bits = BitWriter(blocks)

        // Tabela de códigos: chave (prefixo << 8 | byte) → código, em hash aberto.
        val hashKeys = IntArray(HASH_SIZE) { -1 }
        val hashCodes = IntArray(HASH_SIZE)
        var nextCode = end + 1
        var codeSize = minCodeSize + 1

        fun reset() {
            hashKeys.fill(-1)
            nextCode = end + 1
            codeSize = minCodeSize + 1
        }

        bits.write(clear, codeSize)
        if (indices.isEmpty()) {
            bits.write(end, codeSize)
            bits.flush(); blocks.finish()
            return
        }
        var prefix = indices[0].toInt() and 0xFF
        for (i in 1 until indices.size) {
            val k = indices[i].toInt() and 0xFF
            val key = (prefix shl 8) or k
            var slot = (key * 0x9E3779B1.toInt() ushr 18) and (HASH_SIZE - 1)
            var found = -1
            while (hashKeys[slot] != -1) {
                if (hashKeys[slot] == key) {
                    found = hashCodes[slot]
                    break
                }
                slot = (slot + 1) and (HASH_SIZE - 1)
            }
            if (found >= 0) {
                prefix = found
                continue
            }
            bits.write(prefix, codeSize)
            if (nextCode < MAX_CODES) {
                hashKeys[slot] = key
                hashCodes[slot] = nextCode
                if (nextCode == (1 shl codeSize) && codeSize < MAX_BITS) codeSize++
                nextCode++
            } else {
                bits.write(clear, codeSize)
                reset()
            }
            prefix = k
        }
        bits.write(prefix, codeSize)
        bits.write(end, codeSize)
        bits.flush()
        blocks.finish()
    }

    /** Junta bits (do menos para o mais significativo) em bytes. */
    private class BitWriter(private val blocks: BlockWriter) {
        private var buffer = 0
        private var count = 0
        fun write(code: Int, size: Int) {
            buffer = buffer or (code shl count)
            count += size
            while (count >= 8) {
                blocks.write(buffer and 0xFF)
                buffer = buffer ushr 8
                count -= 8
            }
        }
        fun flush() {
            if (count > 0) blocks.write(buffer and 0xFF)
            buffer = 0
            count = 0
        }
    }

    /** Os dados vão em blocos de até 255 bytes, cada um com o tamanho na frente. */
    private class BlockWriter(private val out: ByteArrayOutputStream) {
        private val block = ByteArray(255)
        private var length = 0
        fun write(byte: Int) {
            block[length++] = byte.toByte()
            if (length == 255) emit()
        }
        private fun emit() {
            if (length == 0) return
            out.write(length)
            out.write(block, 0, length)
            length = 0
        }
        fun finish() {
            emit()
            out.write(0)
        }
    }

    companion object {
        private const val MAX_BITS = 12
        private const val MAX_CODES = 1 shl MAX_BITS
        private const val HASH_SIZE = 1 shl 14

        /**
         * Paleta de até 256 cores, pelas mais comuns nos quadros de amostra
         * (cada cor é a média do seu grupo, em 12 bits por cor).
         */
        fun buildPalette(samples: List<IntArray>): IntArray {
            val count = IntArray(4096)
            val sumR = LongArray(4096)
            val sumG = LongArray(4096)
            val sumB = LongArray(4096)
            for (frame in samples) {
                for (c in frame) {
                    val r = (c shr 16) and 0xFF
                    val g = (c shr 8) and 0xFF
                    val b = c and 0xFF
                    val bin = ((r shr 4) shl 8) or ((g shr 4) shl 4) or (b shr 4)
                    count[bin]++
                    sumR[bin] += r.toLong(); sumG[bin] += g.toLong(); sumB[bin] += b.toLong()
                }
            }
            val top = (0 until 4096).filter { count[it] > 0 }.sortedByDescending { count[it] }.take(256)
            if (top.isEmpty()) return intArrayOf(0)
            return IntArray(top.size) { i ->
                val bin = top[i]
                val n = count[bin]
                val r = (sumR[bin] / n).toInt()
                val g = (sumG[bin] / n).toInt()
                val b = (sumB[bin] / n).toInt()
                (r shl 16) or (g shl 8) or b
            }
        }

        private fun rgb555(c: Int): Int =
            ((c shr 9) and 0x7C00) or ((c shr 6) and 0x03E0) or ((c shr 3) and 0x001F)

        /** Para cada cor em 15 bits, o índice da cor mais parecida da paleta. */
        private fun buildLookup(palette: IntArray): ByteArray {
            val pr = IntArray(palette.size) { (palette[it] shr 16) and 0xFF }
            val pg = IntArray(palette.size) { (palette[it] shr 8) and 0xFF }
            val pb = IntArray(palette.size) { palette[it] and 0xFF }
            return ByteArray(1 shl 15) { key ->
                val r = ((key shr 10) and 0x1F) shl 3 or 4
                val g = ((key shr 5) and 0x1F) shl 3 or 4
                val b = (key and 0x1F) shl 3 or 4
                var best = 0
                var bestDistance = Int.MAX_VALUE
                for (i in pr.indices) {
                    val dr = r - pr[i]
                    val dg = g - pg[i]
                    val db = b - pb[i]
                    // Peso maior no verde, como o olho enxerga.
                    val d = 2 * dr * dr + 4 * dg * dg + 3 * db * db
                    if (d < bestDistance) {
                        bestDistance = d
                        best = i
                    }
                }
                best.toByte()
            }
        }
    }
}
