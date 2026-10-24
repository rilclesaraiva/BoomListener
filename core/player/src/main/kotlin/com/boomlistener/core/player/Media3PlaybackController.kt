// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asSharedFlow
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

    // A fila só muda quando a lista ou o modo aleatório mudam; montá-la a cada
    // atualização de progresso (2x por segundo) seria desperdício.
    private var queueCache: List<QueueEntry> = emptyList()

    // Embaralhamento próprio (ver ShuffleState). O "shuffleModeEnabled" do
    // player só guarda se o aleatório está ligado: o serviço usa uma ordem de
    // embaralhamento neutra, então a fila do player é sempre a ordem real.
    private val shuffle = ShuffleState<MediaItem>(keyOf = { it.mediaId })
    private var lastIndex = C.INDEX_UNSET

    private val _unsupported = MutableSharedFlow<String>(extraBufferCapacity = 4)
    override val unsupported: Flow<String> = _unsupported.asSharedFlow()

    private val listener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            if (error.errorCode !in UNSUPPORTED_FORMAT_ERRORS) return
            val player = controller ?: return
            _unsupported.tryEmit(player.currentMediaItem?.mediaMetadata?.title?.toString().orEmpty())
        }

        override fun onEvents(player: Player, events: Player.Events) {
            if (events.containsAny(
                    Player.EVENT_TIMELINE_CHANGED,
                    Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                )
            ) {
                queueCache = buildQueue(player)
            }
            if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) onTransition(player)
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
                queueCache = buildQueue(connected)
                publish(connected)
                pending.forEach { it(connected) }
                pending.clear()
                if (connected.isPlaying) startProgressUpdates()
            },
            mainExecutor,
        )
    }

    override fun play(tracks: List<Track>, startIndex: Int) = withController { player ->
        val items = tracks.map { it.toMediaItem() }
        // Música escolhida pelo usuário: conta para "mais ouvidas" mesmo no aleatório.
        items.getOrNull(startIndex)?.let { ChosenSongSignal.mark(it.mediaId) }
        shuffle.clear()
        if (player.shuffleModeEnabled && items.isNotEmpty()) {
            // Aleatório continua ligado: a escolhida toca primeiro e o resto
            // vem embaralhado, guardando a ordem da lista para depois.
            player.setMediaItems(shuffle.start(items, startIndex), 0, 0L)
        } else {
            // A escolhida no topo da fila, as outras embaixo dela.
            player.setMediaItems(queueStartingAt(items, startIndex), 0, 0L)
        }
        player.prepare()
        player.play()
    }

    override fun playShuffled(tracks: List<Track>) = withController { player ->
        if (tracks.isEmpty()) return@withController
        val items = tracks.map { it.toMediaItem() }
        shuffle.clear()
        // Tocar no aleatório de novo começa por outra música, nunca pela que já tocava.
        val playing = player.currentMediaItem?.mediaId
        val starts = items.indices.filter { items[it].mediaId != playing }.ifEmpty { items.indices.toList() }
        val order = shuffle.start(items, currentIndex = starts.random())
        player.shuffleModeEnabled = true
        player.setMediaItems(order, 0, 0L)
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

    override fun playQueueItem(index: Int) = withController { player ->
        if (index in 0 until player.mediaItemCount) {
            ChosenSongSignal.mark(player.getMediaItemAt(index).mediaId)
            player.seekToDefaultPosition(index)
            player.play()
        }
    }

    override fun skipToQueueItem(index: Int) = withController { player ->
        if (index in 0 until player.mediaItemCount && index != player.currentMediaItemIndex) {
            player.seekToDefaultPosition(index)
        }
    }

    override fun playNext(track: Track) = withController { player ->
        val item = track.toMediaItem()
        if (player.mediaItemCount == 0) {
            shuffle.clear()
            player.setMediaItems(listOf(item), 0, 0L)
            player.prepare()
            player.play()
            return@withController
        }
        val current = player.currentMediaItem
        // Com o aleatório ligado, entra também na ordem original (logo depois
        // da atual), para continuar lá quando o aleatório for desligado.
        shuffle.insertAfter(current, item)
        player.addMediaItem(player.currentMediaItemIndex + 1, item)
    }

    override fun replaceUpNext(tracks: List<Track>) = withController { player ->
        val count = player.mediaItemCount
        val items = tracks.map { it.toMediaItem() }
        if (count == 0) {
            if (items.isEmpty()) return@withController
            shuffle.clear()
            player.setMediaItems(items, 0, 0L)
            player.prepare()
            player.play()
            return@withController
        }
        // A ordem guardada do aleatório não vale mais: a fila nova já vem misturada.
        shuffle.clear()
        val current = player.currentMediaItemIndex.coerceAtLeast(0)
        if (current + 1 < count) player.removeMediaItems(current + 1, count)
        player.addMediaItems(items)
    }

    override fun moveQueueItem(from: Int, to: Int) = withController { player ->
        val count = player.mediaItemCount
        if (from != to && from in 0 until count && to in 0 until count) player.moveMediaItem(from, to)
    }

    override fun removeQueueItem(index: Int) = withController { player ->
        if (index in 0 until player.mediaItemCount) {
            shuffle.remove(player.getMediaItemAt(index))
            player.removeMediaItem(index)
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

    override fun toggleShuffle() = withController { player ->
        if (player.shuffleModeEnabled) disableShuffle(player) else enableShuffle(player)
    }

    /**
     * Liga o aleatório sem interromper a música: ela vai para o topo da fila e
     * o resto é trocado pela versão embaralhada.
     */
    private fun enableShuffle(player: Player) {
        val count = player.mediaItemCount
        if (count > 0) {
            val current = player.currentMediaItemIndex.coerceAtLeast(0)
            val order = shuffle.start(player.allItems(), current)
            if (current != 0) player.moveMediaItem(current, 0)
            if (count > 1) player.replaceMediaItems(1, count, order.drop(1))
        }
        player.shuffleModeEnabled = true
    }

    /** Desliga o aleatório: volta à ordem original em volta da música atual. */
    private fun disableShuffle(player: Player) {
        val restored = shuffle.stop(player.currentMediaItem)
        if (restored != null) {
            val (before, after) = restored
            val current = player.currentMediaItemIndex
            val count = player.mediaItemCount
            // Primeiro o que vem depois (não mexe na posição da atual), depois o antes.
            player.replaceMediaItems(current + 1, count, after)
            player.replaceMediaItems(0, current, before)
        }
        player.shuffleModeEnabled = false
    }

    /**
     * "Repetir tudo" com aleatório: quando a fila dá a volta (da última para a
     * primeira), embaralha de novo o resto para a próxima rodada.
     */
    private fun onTransition(player: Player) {
        val count = player.mediaItemCount
        val index = player.currentMediaItemIndex
        val wrapped = lastIndex == count - 1 && index == 0 && count > 2
        if (wrapped && player.shuffleModeEnabled && player.repeatMode == Player.REPEAT_MODE_ALL) {
            val round = shuffle.newRound(player.allItems())
            player.replaceMediaItems(1, count, round.drop(1))
        }
        lastIndex = index
    }

    override fun cycleRepeatMode() = withController { player ->
        player.repeatMode = when (player.repeatMode.toRepeatMode().next()) {
            RepeatMode.Off -> Player.REPEAT_MODE_OFF
            RepeatMode.All -> Player.REPEAT_MODE_ALL
            RepeatMode.One -> Player.REPEAT_MODE_ONE
        }
    }

    override fun stop() = withController {
        shuffle.clear()
        it.shuffleModeEnabled = false
        it.stop()
        it.clearMediaItems()
        publish(it)
    }

    override fun updateTrackInfo(uri: String, title: String, artist: String?, album: String?) = withController { player ->
        for (index in 0 until player.mediaItemCount) {
            val item = player.getMediaItemAt(index)
            if (item.mediaId != uri) continue
            val metadata = item.mediaMetadata.buildUpon()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .build()
            // Mesmo arquivo: o ExoPlayer só troca as informações, sem parar.
            player.replaceMediaItem(index, item.buildUpon().setMediaMetadata(metadata).build())
        }
        publish(player)
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
            queue = queueCache,
        )
    }

    /** A fila na ordem real de reprodução. */
    private fun buildQueue(player: Player): List<QueueEntry> {
        val timeline = player.currentTimeline
        if (timeline.isEmpty) return emptyList()
        // A ordem do player já é a ordem real (o embaralhamento é nosso).
        val entries = ArrayList<QueueEntry>(timeline.windowCount)
        val seen = HashMap<String, Int>()
        var index = timeline.getFirstWindowIndex(false)
        while (index != C.INDEX_UNSET && entries.size < timeline.windowCount) {
            val item = player.getMediaItemAt(index).toNowPlayingItem()
            val occurrence = seen.merge(item.uri, 1, Int::plus) ?: 1
            entries += QueueEntry(index, item, key = "${item.uri}#$occurrence")
            index = timeline.getNextWindowIndex(index, Player.REPEAT_MODE_OFF, false)
        }
        return entries
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

private fun Player.allItems(): List<MediaItem> = (0 until mediaItemCount).map(::getMediaItemAt)

private fun MediaItem.toNowPlayingItem(): NowPlayingItem = NowPlayingItem(
    uri = mediaId,
    title = mediaMetadata.title?.toString() ?: "",
    artist = mediaMetadata.artist?.toString(),
    album = mediaMetadata.albumTitle?.toString(),
)

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
