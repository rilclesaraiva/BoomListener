// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.releases

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FollowEncodingTest {
    @Test
    fun idaEVoltaMesmoComBarraNoNome() {
        val artist = FollowedArtist(27, "AC|DC Tribute")
        assertEquals(artist, decodeFollowed(encodeFollowed(artist)))
    }

    @Test
    fun valorInvalidoEIgnorado() {
        assertNull(decodeFollowed("sem-id"))
        assertNull(decodeFollowed("abc|Nome"))
    }
}
