// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.deezer

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppleSearchTest {
    private val response = JSONObject(
        """
        {"resultCount":3,"results":[
          {"kind":"song","trackId":11,"collectionId":5,"trackName":"Beholder","artistName":"Chikoi The Maid",
           "collectionName":"Beholder - Single","trackTimeMillis":181000,
           "trackViewUrl":"https://music.apple.com/us/album/beholder/5?i=11","artworkUrl100":"https://x/100.jpg"},
          {"kind":"music-video","trackId":12,"trackName":"Beholder","artistName":"Chikoi The Maid"},
          {"kind":"song","trackId":13,"collectionId":6,"trackName":"Beholder (Live)","artistName":"Chikoi The Maid",
           "collectionName":"Live","trackTimeMillis":250000,"trackViewUrl":"https://music.apple.com/us/album/live/6?i=13"}
        ]}
        """.trimIndent(),
    )

    @Test
    fun `reads only songs from the apple search`() {
        val songs = parseItunesSongs(response)
        assertEquals(listOf(11L, 13L), songs.map { it.trackId })
        assertEquals("https://music.apple.com/us/album/beholder/5?i=11", songs.first().link)
        assertEquals(181_000L, songs.first().durationMs)
    }

    @Test
    fun `picks the same song, not the live version`() {
        val local = LocalTrackInfo("Beholder", "Chikoi The Maid", album = null, durationMs = 180_500)
        assertEquals(11L, bestShareCandidate(local, parseItunesSongs(response))?.trackId)
    }

    @Test
    fun `another artist is never shared`() {
        val local = LocalTrackInfo("Beholder", "Someone Else", album = null, durationMs = 181_000)
        assertNull(bestShareCandidate(local, parseItunesSongs(response)))
    }
}
