// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseNotesTest {
    @Test
    fun leAsNotasDoChangelog() {
        val md = """
            ### ✨ New
            - **Connected devices.** A new button shows where the sound is.
              - Tap it to see every device.
            - Fast scrolling, see [docs](https://example.com) and `code`.

            ### 🐛 Fixes
            Some text
            on two lines.
        """.trimIndent()
        assertEquals(
            listOf(
                NoteBlock.Heading(3, "✨ New"),
                NoteBlock.Bullet(0, "**Connected devices.** A new button shows where the sound is."),
                NoteBlock.Bullet(1, "Tap it to see every device."),
                NoteBlock.Bullet(0, "Fast scrolling, see docs and code."),
                NoteBlock.Heading(3, "🐛 Fixes"),
                NoteBlock.Paragraph("Some text on two lines."),
            ),
            parseReleaseNotes(md),
        )
    }

    @Test
    fun vazioNaoTemBlocos() {
        assertEquals(emptyList<NoteBlock>(), parseReleaseNotes("  \n\n"))
    }

    @Test
    fun negrito() {
        assertEquals(
            listOf("Connected devices." to true, " A new button." to false),
            boldRuns("**Connected devices.** A new button."),
        )
        assertEquals(listOf("sem fechar" to false), boldRuns("**sem fechar"))
        assertEquals(listOf("nada" to false), boldRuns("nada"))
    }
}
