// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Test

class MenuOrderTest {
    private fun action(label: String, kind: BoomMenuKind) = BoomMenuAction(label, kind = kind) {}

    @Test
    fun `share comes first and delete always last`() {
        val ordered = listOf(
            action("Delete", BoomMenuKind.Delete),
            action("Artist", BoomMenuKind.GoToArtist),
            action("Share", BoomMenuKind.Share),
            action("Something", BoomMenuKind.Other),
            action("Playlist", BoomMenuKind.AddToPlaylist),
        ).inMenuOrder().map { it.label }
        assertEquals(listOf("Share", "Playlist", "Artist", "Something", "Delete"), ordered)
    }

    @Test
    fun `same kind keeps the order the screen gave`() {
        val ordered = listOf(
            action("B", BoomMenuKind.Other),
            action("A", BoomMenuKind.Other),
        ).inMenuOrder().map { it.label }
        assertEquals(listOf("B", "A"), ordered)
    }
}
