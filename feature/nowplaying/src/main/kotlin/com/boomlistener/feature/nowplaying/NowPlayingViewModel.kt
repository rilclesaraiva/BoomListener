// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.boomlistener.core.player.PlaybackController
import com.boomlistener.core.player.PlaybackState
import com.boomlistener.data.library.LibraryRepository
import kotlinx.coroutines.flow.StateFlow

/** Liga as telas do player ao controle de reprodução. */
internal class NowPlayingViewModel(
    private val controller: PlaybackController,
    private val library: LibraryRepository,
) : ViewModel() {

    val state: StateFlow<PlaybackState> = controller.state

    fun togglePlayPause() = controller.togglePlayPause()
    fun playQueueItem(index: Int) = controller.playQueueItem(index)
    fun moveQueueItem(from: Int, to: Int) = controller.moveQueueItem(from, to)
    fun removeQueueItem(index: Int) = controller.removeQueueItem(index)
    fun next() = controller.next()
    fun previous() = controller.previous()
    fun toggleShuffle() = controller.toggleShuffle()
    fun cycleRepeatMode() = controller.cycleRepeatMode()
    fun stop() = controller.stop()

    /** Recebe a posição de 0 a 1 vinda da barra de progresso. */
    fun seekToFraction(fraction: Float) {
        val duration = state.value.durationMs
        if (duration > 0) controller.seekTo((duration * fraction.coerceIn(0f, 1f)).toLong())
    }

    suspend fun artwork(uri: String, sizePx: Int): Bitmap? = library.loadArtwork(uri, sizePx)
}
