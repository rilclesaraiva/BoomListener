// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.profile

import java.io.File
import java.io.FileOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ZipArchiveTest {
    private fun zip(block: (ZipOutputStream) -> Unit): File {
        val file = File.createTempFile("pet", ".zip")
        file.deleteOnExit()
        ZipOutputStream(FileOutputStream(file)).use(block)
        return file
    }

    @Test
    fun readsStoredAndDeflatedEntriesAndOddNames() {
        val stored = "armazenado".toByteArray()
        val deflated = ByteArray(5000) { (it % 7).toByte() }
        val file = zip { out ->
            out.putNextEntry(ZipEntry("/"))
            out.closeEntry()
            out.putNextEntry(ZipEntry("/img/Ankha/shime1.png").apply {
                method = ZipEntry.STORED
                size = stored.size.toLong()
                compressedSize = stored.size.toLong()
                crc = CRC32().apply { update(stored) }.value
            })
            out.write(stored)
            out.closeEntry()
            out.putNextEntry(ZipEntry("conf/actions.xml"))
            out.write(deflated)
            out.closeEntry()
        }
        ZipArchive(file).use { zip ->
            assertEquals(listOf("/", "/img/Ankha/shime1.png", "conf/actions.xml"), zip.entries.map { it.name })
            assertEquals(true, zip.entries[0].isDirectory)
            assertArrayEquals(stored, zip.read(zip.entries[1], 1000))
            assertArrayEquals(deflated, zip.read(zip.entries[2], 10_000))
        }
    }

    @Test
    fun refusesEntriesOverTheLimit() {
        val file = zip { out ->
            out.putNextEntry(ZipEntry("big.png"))
            out.write(ByteArray(5000))
            out.closeEntry()
        }
        ZipArchive(file).use { zip -> assertNull(zip.read(zip.entries[0], 1000)) }
    }
}
