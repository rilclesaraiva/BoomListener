// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.social

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileImagesTest {
    @Test
    fun fotoQuadradaDeUmaImagemDeitada() {
        // 4000×3000 → quadrado de 3000 no meio.
        assertArrayEquals(intArrayOf(500, 0, 3000, 3000), centerCrop(4000, 3000, 320, 320))
    }

    @Test
    fun capaLargaDeUmaFotoEmPe() {
        // 1080×1920 → faixa 1080×405 no meio.
        assertArrayEquals(intArrayOf(0, 757, 1080, 405), centerCrop(1080, 1920, 1200, 450))
    }

    @Test
    fun mesmaProporcaoNaoCorta() {
        assertArrayEquals(intArrayOf(0, 0, 2400, 900), centerCrop(2400, 900, 1200, 450))
    }

    @Test
    fun reducaoNaLeitura() {
        assertEquals(8, sampleSize(4000, 3000, 320, 320))
        assertEquals(1, sampleSize(300, 300, 320, 320))
        assertEquals(2, sampleSize(2400, 1800, 1200, 450))
    }

    @Test
    fun reconheceGif() {
        assertEquals(true, isGif("GIF89a....".toByteArray()))
        assertEquals(true, isGif("GIF87a".toByteArray()))
        assertEquals(false, isGif(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0, 0, 0)))
        assertEquals(false, isGif("GIF".toByteArray()))
    }
}
