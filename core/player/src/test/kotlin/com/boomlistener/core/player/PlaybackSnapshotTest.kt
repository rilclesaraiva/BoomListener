// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackSnapshotTest {
    @Test
    fun idaEVoltaComCaracteresChatos() {
        val snapshot = PlaybackSnapshot(
            items = listOf(
                SnapshotItem("content://1", "Açaí\tao vivo", "Djavan", null),
                SnapshotItem("content://2", "Linha\nnova \\ barra", null, "Luz"),
            ),
            index = 1,
            positionMs = 73_500,
            repeatMode = 2,
        )
        assertEquals(snapshot, decodeSnapshot(encodeSnapshot(snapshot)))
    }

    @Test
    fun arquivoEstranhoOuVazioNaoRestaura() {
        assertNull(decodeSnapshot(""))
        assertNull(decodeSnapshot("outra coisa\n"))
        assertNull(decodeSnapshot("boom-queue-v1\t0\t0\t0\n"))
    }

    @Test
    fun indiceForaDaFilaVoltaParaDentro() {
        val text = "boom-queue-v1\t9\t-5\t0\ncontent://1\tA\t\t\n"
        val snapshot = decodeSnapshot(text)!!
        assertEquals(0, snapshot.index)
        assertEquals(0L, snapshot.positionMs)
    }

    @Test
    fun `shuffle and original order round trip`() {
        val a = SnapshotItem("content://1", "A", null, null)
        val b = SnapshotItem("content://2", "B", "X", null)
        val snapshot = PlaybackSnapshot(listOf(b, a), index = 0, positionMs = 10, repeatMode = 0, shuffle = true, original = listOf(a, b))
        assertEquals(snapshot, decodeSnapshot(encodeSnapshot(snapshot)))
    }

    @Test
    fun `old files without shuffle still read`() {
        val snapshot = decodeSnapshot("boom-queue-v1\t0\t0\t0\ncontent://1\tA\t\t\n")!!
        assertEquals(false, snapshot.shuffle)
        assertEquals(null, snapshot.original)
    }
}
