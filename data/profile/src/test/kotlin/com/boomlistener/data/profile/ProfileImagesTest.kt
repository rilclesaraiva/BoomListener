// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.profile

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

    @Test
    fun reconheceWebp() {
        assertEquals(true, isWebp("RIFF\u0000\u0000\u0000\u0000WEBPVP8 ".toByteArray()))
        assertEquals(false, isWebp("RIFF\u0000\u0000\u0000\u0000WAVEfmt ".toByteArray()))
        assertEquals(false, isWebp("GIF89a".toByteArray()))
    }

    @Test
    fun tamanhoDoGifPeloCabecalho() {
        // 0x01E0 = 480, 0x0168 = 360 (little endian).
        val header = "GIF89a".toByteArray() + byteArrayOf(0xE0.toByte(), 0x01, 0x68, 0x01)
        assertEquals(480 to 360, gifSize(header))
        assertEquals(null, gifSize("RIFF".toByteArray()))
    }

    @Test
    fun gifSoVaiComoEstaSePequenoECurto() {
        val avatar = ProfileImage.Avatar
        assertEquals(true, gifFitsAsIs(1_000_000, 480, 480, 4_000, avatar))
        assertEquals(false, gifFitsAsIs(3_500_000, 480, 480, 4_000, avatar)) // grande demais
        assertEquals(false, gifFitsAsIs(1_000_000, 1920, 1080, 4_000, avatar)) // resolução maior que o necessário
        assertEquals(false, gifFitsAsIs(1_000_000, 480, 480, 40_000, avatar)) // longo demais
        assertEquals(true, gifFitsAsIs(2_000_000, 1080, 405, 10_000, ProfileImage.Banner))
    }
}
