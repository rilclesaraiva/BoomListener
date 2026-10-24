// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import org.junit.Assert.assertEquals
import org.junit.Test

class ShareClipTest {
    @Test
    fun `clip is 30 seconds from the chosen point`() {
        assertEquals(ShareClip(62_000, 92_000), ShareClip.at(62_000, 200_000))
    }

    @Test
    fun `clip near the end moves back to keep 30 seconds`() {
        assertEquals(ShareClip(170_000, 200_000), ShareClip.at(190_000, 200_000))
    }

    @Test
    fun `short song uses the whole song`() {
        assertEquals(ShareClip(0, 20_000), ShareClip.at(5_000, 20_000))
    }

    @Test
    fun `negative start becomes zero`() {
        assertEquals(ShareClip(0, 30_000), ShareClip.at(-3, 100_000))
    }

    @Test
    fun `never longer than 30 seconds`() {
        assertEquals(ShareClip.MAX_MS, ShareClip.at(0, 600_000).lengthMs)
    }
}
