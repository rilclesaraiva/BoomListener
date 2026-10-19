// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.player

import com.boomlistener.core.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class QueueFake(uris: List<String>) : PlaybackController {
    override val state = MutableStateFlow(
        PlaybackState(
            queue = uris.mapIndexed { i, uri -> QueueEntry(i, NowPlayingItem(uri, uri, null, null), "$i") },
            queueSize = uris.size,
        ),
    )
    val removed = mutableListOf<Int>()
    var stopped = false
    override fun play(tracks: List<Track>, startIndex: Int) = Unit
    override fun playShuffled(tracks: List<Track>) = Unit
    override fun togglePlayPause() = Unit
    override fun playQueueItem(index: Int) = Unit
    override fun moveQueueItem(from: Int, to: Int) = Unit
    override fun removeQueueItem(index: Int) {
        removed += index
    }
    override fun next() = Unit
    override fun previous() = Unit
    override fun seekTo(positionMs: Long) = Unit
    override fun toggleShuffle() = Unit
    override fun cycleRepeatMode() = Unit
    override fun stop() {
        stopped = true
    }
}

class RemoveUriTest {
    @Test
    fun tiraTodasAsVezesDeTrasParaFrente() {
        val player = QueueFake(listOf("a", "b", "a", "c"))
        player.removeUri("a")
        assertEquals(listOf(2, 0), player.removed)
    }

    @Test
    fun foraDaFilaNaoMexe() {
        val player = QueueFake(listOf("a"))
        player.removeUri("z")
        assertTrue(player.removed.isEmpty())
    }

    @Test
    fun eraAUnicaParaTudo() {
        val player = QueueFake(listOf("a", "a"))
        player.removeUri("a")
        assertTrue(player.stopped)
        assertTrue(player.removed.isEmpty())
    }
}
