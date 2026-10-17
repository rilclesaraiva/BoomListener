// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import android.graphics.Bitmap
import com.boomlistener.core.model.Track
import com.boomlistener.core.player.PlaybackController
import com.boomlistener.core.player.PlaybackState
import com.boomlistener.data.library.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private class FakeController(duration: Long) : PlaybackController {
    var seekedTo: Long? = null
    override val state: StateFlow<PlaybackState> = MutableStateFlow(PlaybackState(durationMs = duration))
    override fun play(tracks: List<Track>, startIndex: Int) = Unit
    override fun togglePlayPause() = Unit
    override fun next() = Unit
    override fun previous() = Unit
    override fun seekTo(positionMs: Long) { seekedTo = positionMs }
    override fun toggleShuffle() = Unit
    override fun cycleRepeatMode() = Unit
    override fun stop() = Unit
}

private object NoLibrary : LibraryRepository {
    override suspend fun getTracks(): List<Track> = emptyList()
    override suspend fun loadArtwork(trackUri: String, sizePx: Int): Bitmap? = null
}

class NowPlayingViewModelTest {
    @Test
    fun arrastarABarraVaiParaAPosicaoCerta() {
        val controller = FakeController(duration = 200_000)
        NowPlayingViewModel(controller, NoLibrary).seekToFraction(0.25f)
        assertEquals(50_000L, controller.seekedTo)
    }

    @Test
    fun semDuracaoNaoPula() {
        val controller = FakeController(duration = 0)
        NowPlayingViewModel(controller, NoLibrary).seekToFraction(0.5f)
        assertNull(controller.seekedTo)
    }

    @Test
    fun tempoFormatado() {
        assertEquals("3:35", formatTime(215_000))
        assertEquals("1:00:00", formatTime(3_600_000))
    }
}
