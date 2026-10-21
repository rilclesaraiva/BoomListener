// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

class GifEncoderTest {
    private val red = 0xFFFF0000.toInt()
    private val blue = 0xFF0000FF.toInt()
    private val green = 0xFF00FF00.toInt()

    /** Quadro 40×30 com três faixas de cor (e muitos pixels repetidos, como numa imagem real). */
    private fun stripes(first: Int, second: Int, third: Int) = IntArray(40 * 30) { i ->
        when ((i % 40) / 14) {
            0 -> first
            1 -> second
            else -> third
        }
    }

    @Test
    fun gifValidoQueOJavaConsegueLer() {
        val frames = listOf(stripes(red, green, blue), stripes(blue, red, green))
        val encoder = GifEncoder(40, 30, GifEncoder.buildPalette(frames), delayCs = 10)
        frames.forEach(encoder::addFrame)
        val bytes = encoder.finish()

        assertTrue(isGif(bytes))
        assertEquals(0x3B, bytes.last().toInt() and 0xFF)
        val reader = ImageIO.getImageReadersByFormatName("gif").next()
        reader.input = ImageIO.createImageInputStream(ByteArrayInputStream(bytes))
        assertEquals(2, reader.getNumImages(true))
        val first = reader.read(0)
        assertEquals(40, first.width)
        assertEquals(30, first.height)
        assertEquals(red, first.getRGB(0, 0))
        assertEquals(green, first.getRGB(20, 10))
        assertEquals(blue, first.getRGB(39, 29))
        val second = reader.read(1)
        assertEquals(blue, second.getRGB(0, 0))
    }

    @Test
    fun imagemGrandeComMuitasCoresPassaPeloLimiteDaTabela() {
        // 200×200 com degradê: força a tabela do LZW a encher e zerar.
        val frame = IntArray(200 * 200) { i -> 0xFF000000.toInt() or ((i * 37) and 0xFFFFFF) }
        val encoder = GifEncoder(200, 200, GifEncoder.buildPalette(listOf(frame)), delayCs = 10)
        encoder.addFrame(frame)
        val image = ImageIO.read(ByteArrayInputStream(encoder.finish()))
        assertEquals(200, image.width)
    }

    @Test
    fun quadrosPorSegundoCabemNoLimite() {
        // Capa (480×180) com 15 s (o máximo): poucos quadros por segundo.
        assertEquals(5, framesPerSecondFor(15_000, 480 * 180, MAX_GIF_BYTES))
        // Foto curta: até 10.
        assertEquals(10, framesPerSecondFor(5_000, 240 * 240, MAX_GIF_BYTES))
        // Nunca menos de 4.
        assertEquals(4, framesPerSecondFor(30_000, 1200 * 450, MAX_GIF_BYTES))
    }
}
