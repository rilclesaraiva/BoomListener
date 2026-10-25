// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BackupTracksTest {
    private fun track(
        id: Int,
        file: String = "song$id.mp3",
        folder: String = "Music",
        title: String = "Song $id",
        artist: String = "Artist",
        album: String = "Album",
        duration: Long = 200_000,
    ) = BackupTrack("content://media/external/audio/media/$id", title, artist, album, duration, file, folder)

    @Test
    fun `finds uris in escaped and plain json`() {
        val text = """{"a":"content:\/\/media\/external\/audio\/media\/12","b":"content://media/external/audio/media/7"}"""
        assertEquals(
            setOf("content://media/external/audio/media/12", "content://media/external/audio/media/7"),
            mediaUrisIn(text),
        )
    }

    @Test
    fun `remap keeps the json escaping`() {
        val text = """["content:\/\/media\/external\/audio\/media\/12","content://media/external/audio/media/7","content://media/external/audio/media/99"]"""
        val map = mapOf(
            "content://media/external/audio/media/12" to "content://media/external/audio/media/500",
            "content://media/external/audio/media/7" to "content://media/external/audio/media/501",
        )
        assertEquals(
            """["content:\/\/media\/external\/audio\/media\/500","content://media/external/audio/media/501","content://media/external/audio/media/99"]""",
            remapMediaUris(text, map),
        )
    }

    @Test
    fun `remap does not touch a longer id`() {
        val map = mapOf("content://media/external/audio/media/1" to "content://media/external/audio/media/9")
        assertEquals("content://media/external/audio/media/12", remapMediaUris("content://media/external/audio/media/12", map))
    }

    @Test
    fun `same phone keeps the same uri`() {
        val old = track(1)
        assertEquals(mapOf(old.uri to old.uri), matchBackupTracks(listOf(old), listOf(old)))
    }

    @Test
    fun `new phone matches by folder and file name`() {
        val old = track(1, file = "Beholder.flac", folder = "Chikoi")
        val now = track(830, file = "Beholder.flac", folder = "chikoi")
        assertEquals(mapOf(old.uri to now.uri), matchBackupTracks(listOf(old), listOf(track(5), now)))
    }

    @Test
    fun `reused id with another file is not trusted`() {
        val old = track(1, file = "a.mp3", title = "A")
        val other = track(1, file = "b.mp3", title = "B")
        val moved = track(40, file = "a.mp3", folder = "Downloads", title = "A")
        assertEquals(mapOf(old.uri to moved.uri), matchBackupTracks(listOf(old), listOf(other, moved)))
    }

    @Test
    fun `renamed file still matches by title and artist with accents`() {
        val old = track(1, file = "01 - musica.mp3", title = "Música", artist = "João", duration = 180_000)
        val now = track(77, file = "Musica (remaster).mp3", title = "musica", artist = "JOAO", duration = 181_500)
        assertEquals(mapOf(old.uri to now.uri), matchBackupTracks(listOf(old), listOf(now)))
    }

    @Test
    fun `different duration is another recording`() {
        val old = track(1, file = "x.mp3", title = "Song", duration = 180_000)
        val live = track(2, file = "y.mp3", title = "Song", duration = 260_000)
        assertFalse(matchBackupTracks(listOf(old), listOf(live)).containsKey(old.uri))
    }

    @Test
    fun `missing song stays out of the map`() {
        assertEquals(emptyMap<String, String>(), matchBackupTracks(listOf(track(1)), emptyList()))
    }
}
