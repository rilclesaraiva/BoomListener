// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.tageditor

import android.graphics.Bitmap
import com.boomlistener.core.model.TagSuggestion
import com.boomlistener.core.model.Track
import com.boomlistener.core.model.TrackTags
import com.boomlistener.core.player.PlaybackController
import com.boomlistener.core.player.PlaybackState
import com.boomlistener.data.deezer.AlbumResult
import com.boomlistener.data.deezer.DeezerRepository
import com.boomlistener.data.deezer.LocalTrackInfo
import com.boomlistener.data.deezer.ProfileResult
import com.boomlistener.data.deezer.TrackCandidate
import com.boomlistener.data.deezer.TrackSearchResult
import com.boomlistener.data.deezer.TrackTagsResult
import com.boomlistener.data.lastfm.BioResult
import com.boomlistener.data.lastfm.LastfmRepository
import com.boomlistener.data.library.LibraryRepository
import com.boomlistener.data.tags.CoverEdit
import com.boomlistener.data.tags.TagsReadResult
import com.boomlistener.data.tags.TagsRepository
import com.boomlistener.data.tags.TagsWriteResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val URI = "content://media/external/audio/media/7"

private val inFile = TrackTags(title = "Catharsis", artist = "Chikoi The Maid", lyrics = "letra minha")

private class FakeTags(var writeResult: TagsWriteResult = TagsWriteResult.Success) : TagsRepository {
    var written: Pair<TrackTags, CoverEdit>? = null
    override suspend fun read(uri: String) = TagsReadResult.Found(inFile, cover = null, durationMs = 180_000)
    override suspend fun readLyrics(uri: String): String? = null
    override suspend fun write(uri: String, tags: TrackTags, cover: CoverEdit): TagsWriteResult {
        written = tags to cover
        return writeResult
    }
}

private class FakeLibrary : LibraryRepository {
    val changed = mutableListOf<String>()
    override suspend fun getTracks(): List<Track> = emptyList()
    override suspend fun loadArtwork(trackUri: String, sizePx: Int): Bitmap? = null
    override fun notifyChanged(trackUri: String) {
        changed += trackUri
    }
}

private fun candidate(id: Long, confidence: Float) = TrackCandidate(
    trackId = id, albumId = id * 10, title = "Catharsis", artist = "Chikoi The Maid",
    album = "Insertion", coverUrl = "https://cdn/cover$id.jpg", durationMs = 180_000, link = null,
    confidence = confidence,
)

private class FakeDeezer(private val candidates: List<TrackCandidate>) : DeezerRepository {
    override suspend fun artistProfile(artistName: String) = ProfileResult.NotFound
    override suspend fun album(albumId: Long) = AlbumResult.Failed
    override suspend fun image(url: String, sizePx: Int): Bitmap? = null
    override suspend fun searchTrack(local: LocalTrackInfo) =
        if (candidates.isEmpty()) TrackSearchResult.NotFound else TrackSearchResult.Found(candidates)
    override suspend fun trackTags(candidate: TrackCandidate) = TrackTagsResult.Found(
        TagSuggestion(
            tags = TrackTags(
                title = "Catharsis", artist = "Chikoi The Maid", album = "Insertion", albumArtist = "Chikoi The Maid",
                year = "2024", genre = "Rock", trackNumber = "4", trackTotal = "9", discNumber = "1",
            ),
            coverUrl = candidate.coverUrl, durationMs = 180_000, confidence = candidate.confidence,
            source = "Deezer", link = null,
        ),
    )
    override suspend fun imageBytes(url: String): ByteArray = byteArrayOf(1, 2, 3)
}

private class FakeLastfm(private val tags: List<String>) : LastfmRepository {
    override suspend fun artistBio(artistName: String, language: String) = BioResult.NotFound
    override suspend fun trackTags(artistName: String, title: String) = tags
}

private class FakePlayer : PlaybackController {
    var updated: String? = null
    override val state: StateFlow<PlaybackState> = MutableStateFlow(PlaybackState())
    override fun play(tracks: List<Track>, startIndex: Int) = Unit
    override fun playShuffled(tracks: List<Track>) = Unit
    override fun togglePlayPause() = Unit
    override fun playQueueItem(index: Int) = Unit
    override fun moveQueueItem(from: Int, to: Int) = Unit
    override fun removeQueueItem(index: Int) = Unit
    override fun next() = Unit
    override fun previous() = Unit
    override fun seekTo(positionMs: Long) = Unit
    override fun toggleShuffle() = Unit
    override fun cycleRepeatMode() = Unit
    override fun stop() = Unit
    override fun updateTrackInfo(uri: String, title: String, artist: String?, album: String?) {
        updated = title
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TagEditorViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        candidates: List<TrackCandidate> = listOf(candidate(1, 0.95f)),
        lastfmTags: List<String> = listOf("seen live", "math rock"),
        tags: FakeTags = FakeTags(),
        library: FakeLibrary = FakeLibrary(),
        player: FakePlayer = FakePlayer(),
    ) = TagEditorViewModel(URI, tags, library, FakeDeezer(candidates), FakeLastfm(lastfmTags), player)

    @Test
    fun carregaSemAlteracoes() {
        val state = viewModel().state.value
        assertFalse(state.loading)
        assertEquals(inFile, state.tags)
        assertFalse(state.hasChanges)
    }

    @Test
    fun resultadoClaroPreencheSozinhoComGeneroDoLastfmECapa() {
        val vm = viewModel()
        vm.autoTag()
        val state = vm.state.value
        assertTrue(state.autoTag is AutoTagState.Applied)
        assertEquals("Insertion", state.tags.album)
        assertEquals("4", state.tags.trackNumber)
        assertEquals("Math Rock", state.tags.genre) // Last.fm, mais preciso que o "Rock" do Deezer
        assertEquals("letra minha", state.tags.lyrics) // a fonte não tem letra: mantém a do arquivo
        assertArrayEquals(byteArrayOf(1, 2, 3), state.shownCover)
        assertTrue(state.hasChanges)
    }

    @Test
    fun semGeneroNoLastfmUsaODoDeezer() {
        val vm = viewModel(lastfmTags = emptyList())
        vm.autoTag()
        assertEquals("Rock", vm.state.value.tags.genre)
    }

    @Test
    fun duvidaMostraOpcoesEEscolherPreenche() {
        val vm = viewModel(candidates = listOf(candidate(1, 0.7f), candidate(2, 0.65f)))
        vm.autoTag()
        assertTrue(vm.state.value.autoTag is AutoTagState.Choosing)
        vm.choose(candidate(2, 0.65f))
        val applied = vm.state.value.autoTag as AutoTagState.Applied
        assertEquals(2L, applied.chosen.trackId)
        assertEquals(listOf(1L), applied.others.map { it.trackId })
    }

    @Test
    fun desfazerVoltaAoQueEstava() {
        val vm = viewModel()
        vm.edit { it.copy(title = "Catharsis (meu)") }
        vm.autoTag()
        vm.undoAutoTag()
        assertEquals("Catharsis (meu)", vm.state.value.tags.title)
        assertEquals(CoverState.Original, vm.state.value.cover)
    }

    @Test
    fun naoEncontrado() {
        val vm = viewModel(candidates = emptyList())
        vm.autoTag()
        assertEquals(AutoTagState.NotFound, vm.state.value.autoTag)
    }

    @Test
    fun salvarAvisaBibliotecaEPlayer() {
        val tags = FakeTags()
        val library = FakeLibrary()
        val player = FakePlayer()
        val vm = viewModel(tags = tags, library = library, player = player)
        vm.edit { it.copy(title = "Catarse") }
        vm.save()
        assertEquals("Catarse", tags.written?.first?.title)
        assertEquals(CoverEdit.Keep, tags.written?.second)
        assertEquals(listOf(URI), library.changed)
        assertEquals("Catarse", player.updated)
        assertFalse(vm.state.value.hasChanges) // o que foi gravado vira o "original"
    }

    @Test
    fun falhaAoSalvarMantemAsAlteracoes() {
        val vm = viewModel(tags = FakeTags(TagsWriteResult.Failed))
        vm.edit { it.copy(title = "Catarse") }
        vm.save()
        assertTrue(vm.state.value.hasChanges)
        assertFalse(vm.state.value.saving)
    }
}
