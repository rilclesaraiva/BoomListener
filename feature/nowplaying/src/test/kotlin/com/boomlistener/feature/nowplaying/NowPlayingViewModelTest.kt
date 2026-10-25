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
import org.junit.After
import org.junit.Before
import com.boomlistener.core.model.LyricLine
import com.boomlistener.core.model.Lyrics
import com.boomlistener.core.player.NowPlayingItem
import com.boomlistener.data.lyrics.LyricsQuery
import com.boomlistener.data.lyrics.LyricsRepository
import com.boomlistener.data.lyrics.LyricsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

private class FakeController(duration: Long, item: NowPlayingItem? = null) : PlaybackController {
    var seekedTo: Long? = null
    var playedIndex: Int? = null
    var moved: Pair<Int, Int>? = null
    var removed: Int? = null
    override val state: StateFlow<PlaybackState> = MutableStateFlow(PlaybackState(item = item, durationMs = duration))
    override fun play(tracks: List<Track>, startIndex: Int) = Unit
    override fun playShuffled(tracks: List<Track>) = Unit
    override fun togglePlayPause() = Unit
    override fun playQueueItem(index: Int) { playedIndex = index }
    override fun moveQueueItem(from: Int, to: Int) { moved = from to to }
    override fun removeQueueItem(index: Int) { removed = index }
    override fun next() = Unit
    override fun previous() = Unit
    override fun seekTo(positionMs: Long) { seekedTo = positionMs }
    override fun toggleShuffle() = Unit
    override fun cycleRepeatMode() = Unit
    override fun stop() = Unit
}

private class FakeLyrics(var result: LyricsResult = LyricsResult.NotFound) : LyricsRepository {
    var calls = 0
    override suspend fun getLyrics(query: LyricsQuery): LyricsResult {
        calls++
        return result
    }
}

private object NoLibrary : LibraryRepository {
    override suspend fun getTracks(): List<Track> = emptyList()
    override suspend fun loadArtwork(trackUri: String, sizePx: Int): Bitmap? = null
}

@OptIn(ExperimentalCoroutinesApi::class)
class NowPlayingViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun letraSoEBuscadaQuandoAbreEFicaPronta() {
        val song = NowPlayingItem("content://1", "Açaí", "Djavan", "Luz")
        val lyrics = Lyrics(listOf(LyricLine(0, "Açaí, guardiã")), synced = true, source = "LRCLIB")
        val repo = FakeLyrics(LyricsResult.Found(lyrics))
        val viewModel = NowPlayingViewModel(FakeController(duration = 200_000, item = song), NoLibrary, repo)

        assertEquals(0, repo.calls) // privacidade: nada é buscado com a letra fechada
        viewModel.toggleLyrics()
        assertEquals(1, repo.calls)
        assertEquals(LyricsUiState.Ready("content://1", lyrics), viewModel.lyrics.value)

        // Fechar e abrir de novo na mesma música não busca outra vez.
        viewModel.toggleLyrics()
        viewModel.toggleLyrics()
        assertEquals(1, repo.calls)
    }

    @Test
    fun letraDoArquivoVemAntesDaInternet() {
        val song = NowPlayingItem("content://1", "Açaí", "Djavan", "Luz")
        val repo = FakeLyrics(LyricsResult.NotFound)
        val viewModel = NowPlayingViewModel(
            FakeController(duration = 200_000, item = song),
            NoLibrary,
            repo,
            embeddedLyrics = { "[00:01.00] Açaí, guardiã" },
        )
        viewModel.toggleLyrics()
        assertEquals(0, repo.calls)
        val state = viewModel.lyrics.value as LyricsUiState.Ready
        assertEquals("Açaí, guardiã", state.lyrics.lines.single().text)
        assertEquals(true, state.lyrics.synced)
    }

    @Test
    fun internetPrimeiroQuandoEscolhido() {
        val song = NowPlayingItem("content://1", "Açaí", "Djavan", "Luz")
        val online = Lyrics(listOf(LyricLine(0, "da internet")), synced = true, source = "LRCLIB")
        val repo = FakeLyrics(LyricsResult.Found(online))
        val viewModel = NowPlayingViewModel(
            FakeController(duration = 200_000, item = song),
            NoLibrary,
            repo,
            embeddedLyrics = { "[00:01.00] do arquivo" },
            onlineLyricsFirst = { true },
        )
        viewModel.toggleLyrics()
        assertEquals(1, repo.calls)
        assertEquals(LyricsUiState.Ready("content://1", online), viewModel.lyrics.value)
    }

    @Test
    fun internetPrimeiroSemResultadoUsaOArquivo() {
        val song = NowPlayingItem("content://1", "Açaí", "Djavan", "Luz")
        val repo = FakeLyrics(LyricsResult.Failed)
        val viewModel = NowPlayingViewModel(
            FakeController(duration = 200_000, item = song),
            NoLibrary,
            repo,
            embeddedLyrics = { "[00:01.00] do arquivo" },
            onlineLyricsFirst = { true },
        )
        viewModel.toggleLyrics()
        val state = viewModel.lyrics.value as LyricsUiState.Ready
        assertEquals("do arquivo", state.lyrics.lines.single().text)
    }

    @Test
    fun tentarDeNovoDepoisDeFalha() {
        val song = NowPlayingItem("content://1", "Açaí", "Djavan", "Luz")
        val repo = FakeLyrics(LyricsResult.Failed)
        val viewModel = NowPlayingViewModel(FakeController(duration = 200_000, item = song), NoLibrary, repo)
        viewModel.toggleLyrics()
        assertEquals(LyricsUiState.Failed, viewModel.lyrics.value)
        repo.result = LyricsResult.NotFound
        viewModel.retryLyrics()
        assertEquals(LyricsUiState.NotFound, viewModel.lyrics.value)
    }

    @Test
    fun arrastarABarraVaiParaAPosicaoCerta() {
        val controller = FakeController(duration = 200_000)
        NowPlayingViewModel(controller, NoLibrary, FakeLyrics()).seekToFraction(0.25f)
        assertEquals(50_000L, controller.seekedTo)
    }

    @Test
    fun semDuracaoNaoPula() {
        val controller = FakeController(duration = 0)
        NowPlayingViewModel(controller, NoLibrary, FakeLyrics()).seekToFraction(0.5f)
        assertNull(controller.seekedTo)
    }

    @Test
    fun tocarDaFilaUsaAPosicaoOriginal() {
        val controller = FakeController(duration = 1000)
        NowPlayingViewModel(controller, NoLibrary, FakeLyrics()).playQueueItem(7)
        assertEquals(7, controller.playedIndex)
    }

    @Test
    fun reordenarERemoverChegamAoPlayer() {
        val controller = FakeController(duration = 1000)
        val viewModel = NowPlayingViewModel(controller, NoLibrary, FakeLyrics())
        viewModel.moveQueueItem(2, 9)
        viewModel.removeQueueItem(4)
        assertEquals(2 to 9, controller.moved)
        assertEquals(4, controller.removed)
    }

    @Test
    fun tempoFormatado() {
        assertEquals("3:35", formatTime(215_000))
        assertEquals("1:00:00", formatTime(3_600_000))
    }
}
