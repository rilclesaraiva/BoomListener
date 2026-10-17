// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.deezer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SongLinksTest {
    @Test
    fun musicaViraWatchComDrli() {
        assertEquals(
            "https://music.youtube.com/watch?v=U4PI7v6inYc&drli=1",
            youtubeMusicShareUrl("https://music.youtube.com/watch?v=U4PI7v6inYc"),
        )
        assertEquals(
            "https://music.youtube.com/watch?v=U4PI7v6inYc&drli=1",
            youtubeMusicShareUrl("https://www.youtube.com/watch?feature=share&v=U4PI7v6inYc"),
        )
        assertEquals(
            "https://music.youtube.com/watch?v=U4PI7v6inYc&drli=1",
            youtubeMusicShareUrl("https://youtu.be/U4PI7v6inYc"),
        )
    }

    @Test
    fun albumFicaComoVeio() {
        val album = "https://music.youtube.com/playlist?list=OLAK5uy_abc"
        assertEquals(album, youtubeMusicShareUrl(album))
    }

    @Test
    fun outroSiteNaoServe() {
        assertNull(youtubeMusicShareUrl("https://www.deezer.com/track/1"))
    }
}
