// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.boomlistener.core.player.PlaybackController
import com.boomlistener.core.player.PlaybackState
import com.boomlistener.data.library.LibraryRepository
import androidx.lifecycle.viewModelScope
import com.boomlistener.core.model.Lyrics
import com.boomlistener.data.lyrics.LyricsQuery
import com.boomlistener.data.lyrics.LyricsRepository
import com.boomlistener.data.lyrics.LyricsResult
import com.boomlistener.data.lyrics.lyricsFromText
import com.boomlistener.core.common.ExternalLinks
import com.boomlistener.core.player.NowPlayingItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Estado da letra da música atual. */
internal sealed interface LyricsUiState {
    data object Idle : LyricsUiState
    data object Loading : LyricsUiState
    data class Ready(val uri: String, val lyrics: Lyrics) : LyricsUiState
    data object NotFound : LyricsUiState
    data object Failed : LyricsUiState
}

/** Liga as telas do player ao controle de reprodução. */
internal class NowPlayingViewModel(
    private val controller: PlaybackController,
    private val library: LibraryRepository,
    private val lyricsRepository: LyricsRepository,
    /** Letra guardada dentro do arquivo (tag), que tem prioridade sobre a internet. */
    private val embeddedLyrics: suspend (uri: String) -> String? = { null },
    /** Link do YouTube Music de uma música do aparelho (null se não achar). */
    private val songLink: suspend (title: String, artist: String?, durationMs: Long) -> String? = { _, _, _ -> null },
) : ViewModel() {

    val state: StateFlow<PlaybackState> = controller.state

    /** Sobe quando tags foram editadas (capa e letra são lidas de novo). */
    val libraryVersion: StateFlow<Int> = library.version

    /** Mostrando a letra no lugar da capa? */
    private val _showLyrics = MutableStateFlow(false)
    val showLyrics: StateFlow<Boolean> = _showLyrics.asStateFlow()

    private val _lyrics = MutableStateFlow<LyricsUiState>(LyricsUiState.Idle)
    // Cada "tentar de novo" muda este número e dispara uma nova busca.
    private val retries = MutableStateFlow(0)
    // Versão da biblioteca da última letra carregada (letra editada = buscar de novo).
    private var lyricsVersion = -1
    val lyrics: StateFlow<LyricsUiState> = _lyrics.asStateFlow()

    init {
        // A letra só é buscada com a letra aberta (privacidade: nada sai do
        // aparelho sem o usuário pedir) e de novo a cada troca de música.
        viewModelScope.launch {
            combine(
                _showLyrics,
                controller.state.map { it.item }.distinctUntilChanged(),
                retries,
                library.version,
            ) { show, item, _, version -> Triple(show, item, version) }.collectLatest { (show, item, version) ->
                if (!show || item == null) return@collectLatest
                val current = _lyrics.value
                if (current is LyricsUiState.Ready && current.uri == item.uri && version == lyricsVersion) {
                    return@collectLatest
                }
                lyricsVersion = version
                _lyrics.value = LyricsUiState.Loading

                // 1º: a letra guardada no próprio arquivo (não precisa de internet).
                val fromFile = runCatching { lyricsFromText(embeddedLyrics(item.uri)) }.getOrNull()
                if (fromFile != null) {
                    _lyrics.value = LyricsUiState.Ready(item.uri, fromFile)
                    return@collectLatest
                }

                // 2º: LRCLIB. Dá um instante para a duração da música chegar do player.
                if (controller.state.value.durationMs <= 0) delay(400)
                val query = LyricsQuery(
                    title = item.title,
                    artist = item.artist,
                    album = item.album,
                    durationMs = controller.state.value.durationMs,
                )
                _lyrics.value = when (val result = lyricsRepository.getLyrics(query)) {
                    is LyricsResult.Found -> LyricsUiState.Ready(item.uri, result.lyrics)
                    LyricsResult.NotFound -> LyricsUiState.NotFound
                    LyricsResult.Failed -> LyricsUiState.Failed
                }
            }
        }
    }

    /** Link para compartilhar a música atual: no YouTube Music, ou a busca por ela. */
    suspend fun shareUrl(item: NowPlayingItem): String =
        runCatching { songLink(item.title, item.artist, controller.state.value.durationMs) }.getOrNull()
            ?: ExternalLinks.youtubeMusicSearch(item.title, item.artist)

    fun toggleLyrics() {
        _showLyrics.value = !_showLyrics.value
    }

    /** Tentar de novo depois de uma falha (ex.: estava sem internet). */
    fun retryLyrics() {
        _lyrics.value = LyricsUiState.Idle
        retries.value += 1
    }

    fun togglePlayPause() = controller.togglePlayPause()
    fun playQueueItem(index: Int) = controller.playQueueItem(index)
    fun moveQueueItem(from: Int, to: Int) = controller.moveQueueItem(from, to)
    fun removeQueueItem(index: Int) = controller.removeQueueItem(index)
    fun next() = controller.next()
    fun previous() = controller.previous()
    fun toggleShuffle() = controller.toggleShuffle()
    fun cycleRepeatMode() = controller.cycleRepeatMode()
    fun seekTo(positionMs: Long) = controller.seekTo(positionMs)
    fun stop() = controller.stop()

    /** Recebe a posição de 0 a 1 vinda da barra de progresso. */
    fun seekToFraction(fraction: Float) {
        val duration = state.value.durationMs
        if (duration > 0) controller.seekTo((duration * fraction.coerceIn(0f, 1f)).toLong())
    }

    suspend fun artwork(uri: String, sizePx: Int): Bitmap? = library.loadArtwork(uri, sizePx)
}
