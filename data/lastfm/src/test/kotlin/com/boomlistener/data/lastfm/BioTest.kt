// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.lastfm

import org.junit.Assert.assertEquals
import org.junit.Test

class BioTest {
    @Test
    fun tiraLinkELicencaEHtml() {
        val raw = "Djavan é um cantor <b>brasileiro</b> &amp; compositor. " +
            "<a href=\"https://www.last.fm/music/Djavan\">Read more on Last.fm</a>. " +
            "User-contributed text is available under the Creative Commons By-SA License."
        assertEquals("Djavan é um cantor brasileiro & compositor.", cleanBio(raw))
    }
}
