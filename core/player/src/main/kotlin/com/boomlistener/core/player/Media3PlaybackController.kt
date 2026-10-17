// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.boomlistener.core.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

internal class Media3PlaybackController(context: Context) : PlaybackController {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val mainExecutor = Executor { mainHandler.post(it) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var controller: MediaController? = null
    // Comandos dados antes da conexão com o serviço terminar.
    private val pending = mutableListOf<(MediaController) -> Unit>()
    private var progressJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publish(player)
            if (player.isPlaying) startProgressUpdates() else stopProgressUpdates()
        }
    }

    init {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                val connected = runCatching { future.get() }.getOrNull() ?: return@addListener
                controller = connected
                connected.addListener(listener)
                publish(connected)
                pending.forEach { it(connected) }
                pending.clear()
                if (connected.isPlaying) startProgressUpdates()
            },
            mainExecutor,
        )
    }

    override fun play(tracks: List<Track>, startIndex: Int) = withController { player ->
        player.setMediaItems(tracks.map { it.toMediaItem() }, startIndex, 0L)
        player.prepare()
        player.play()
    }

    override fun togglePlayPause() = withController { player ->
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_ENDED) player.seekToDefaultPosition()
            player.play()
        }
    }

    override fun next() = withController { it.seekToNext() }

    // Igual aos players comuns: depois de 3 s volta ao começo da música; antes
    // disso, vai para a anterior.
    override fun previous() = withController { it.seekToPrevious() }

    override fun seekTo(positionMs: Long) = withController {
        it.seekTo(positionMs)
        publish(it)
    }

    override fun toggleShuffle() = withController { it.shuffleModeEnabled = !it.shuffleModeEnabled }

    override fun cycleRepeatMode() = withController { player ->
        player.repeatMode = when (player.repeatMode.toRepeatMode().next()) {
            RepeatMode.Off -> Player.REPEAT_MODE_OFF
            RepeatMode.All -> Player.REPEAT_MODE_ALL
            RepeatMode.One -> Player.REPEAT_MODE_ONE
        }
    }

    /** Executa já, ou assim que a conexão com o serviço ficar pronta. */
    private fun withController(command: (MediaController) -> Unit) {
        mainHandler.post {
            val current = controller
            if (current != null) command(current) else pending += command
        }
    }

    private fun publish(player: Player) {
        val metadata = player.mediaMetadata
        val currentItem = player.currentMediaItem
        _state.value = PlaybackState(
            item = currentItem?.let {
                NowPlayingItem(
                    uri = it.mediaId,
                    title = metadata.title?.toString() ?: "",
                    artist = metadata.artist?.toString(),
                    album = metadata.albumTitle?.toString(),
                )
            },
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0),
            durationMs = player.duration.takeIf { it > 0 } ?: 0,
            shuffle = player.shuffleModeEnabled,
            repeatMode = player.repeatMode.toRepeatMode(),
            queueIndex = player.currentMediaItemIndex,
            queueSize = player.mediaItemCount,
        )
    }

    private fun startProgressUpdates() {
        if (progressJob?.isActive == true) return
        progressJob = scope.launch {
            while (isActive) {
                controller?.let { publish(it) }
                delay(500)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }
}

private fun Int.toRepeatMode(): RepeatMode = when (this) {
    Player.REPEAT_MODE_ONE -> RepeatMode.One
    Player.REPEAT_MODE_ALL -> RepeatMode.All
    else -> RepeatMode.Off
}

/** A música vira um item do Media3. O mediaId é o próprio endereço do arquivo. */
private fun Track.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(uri)
        .setUri(Uri.parse(uri))
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setTrackNumber(trackNumber)
                .setRecordingYear(year)
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .build(),
        )
        .build()
