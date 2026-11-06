// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.tageditor

import android.graphics.Bitmap
import com.boomlistener.core.model.Track
import com.boomlistener.data.library.LibraryRepository
import com.boomlistener.data.tags.CoverEdit
import com.boomlistener.data.tags.TagGap
import com.boomlistener.data.tags.TagsReadResult
import com.boomlistener.data.tags.TagsRepository
import com.boomlistener.data.tags.TagsWriteResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private fun track(id: Long, title: String, artist: String?) = Track(
    id = id, uri = "content://media/$id", title = title, artist = artist, album = null, albumId = 0,
    durationMs = 1000, trackNumber = null, year = null, dateAddedSeconds = 0,
)

private class ScanTags(val results: MutableMap<String, Set<TagGap>?>) : TagsRepository {
    val scanned = mutableListOf<String>()
    override suspend fun read(uri: String) = TagsReadResult.Failed
    override suspend fun readLyrics(uri: String): String? = null
    override suspend fun write(uri: String, tags: com.boomlistener.core.model.TrackTags, cover: CoverEdit) = TagsWriteResult.Failed
    override suspend fun scan(uri: String): Set<TagGap>? {
        scanned += uri
        return results[uri]
    }
}

private class ListLibrary(private val tracks: List<Track>) : LibraryRepository {
    override suspend fun getTracks(): List<Track> = tracks
    override suspend fun loadArtwork(trackUri: String, sizePx: Int): Bitmap? = null
}

@OptIn(ExperimentalCoroutinesApi::class)
class TagAuditViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val tracks = listOf(
        track(1, "Açaí", "Djavan"),
        track(2, "Song (feat. X)", "A, B"),
        track(3, "Quebrada", null),
    )

    private fun viewModel(tags: ScanTags) = TagAuditViewModel(tags, ListLibrary(tracks), io = dispatcher)

    @Test
    fun agrupaPeloQueFalta() {
        val tags = ScanTags(
            mutableMapOf(
                "content://media/1" to emptySet(),
                "content://media/2" to setOf(TagGap.Cover, TagGap.Lyrics),
                "content://media/3" to setOf(TagGap.Lyrics),
            ),
        )
        val vm = viewModel(tags)
        vm.scan()
        val state = vm.state.value
        assertEquals(ScanPhase.Done, state.phase)
        assertEquals(3, state.scanned)
        assertEquals(listOf(TagGap.Cover, TagGap.Lyrics), state.found.keys.toList())
        assertEquals(listOf("Song (feat. X)"), state.found.getValue(TagGap.Cover).map { it.title })
        assertEquals(listOf("Song (feat. X)", "Quebrada"), state.found.getValue(TagGap.Lyrics).map { it.title })
    }

    @Test
    fun arquivoIlegivelNaoContaComoFalta() {
        val tags = ScanTags(
            mutableMapOf(
                "content://media/1" to null,
                "content://media/2" to emptySet(),
                "content://media/3" to emptySet(),
            ),
        )
        val vm = viewModel(tags)
        vm.scan()
        assertEquals(1, vm.state.value.unreadable)
        assertTrue(vm.state.value.found.isEmpty())
    }

    @Test
    fun depoisDeEditarSoLeDeNovoAsAbertas() {
        val tags = ScanTags(
            mutableMapOf(
                "content://media/1" to setOf(TagGap.Genre),
                "content://media/2" to setOf(TagGap.Genre),
                "content://media/3" to emptySet(),
            ),
        )
        val vm = viewModel(tags)
        vm.scan()
        tags.scanned.clear()
        tags.results["content://media/1"] = emptySet()
        vm.markEdited(listOf("content://media/1"))
        vm.refreshEdited()
        assertEquals(listOf("content://media/1"), tags.scanned)
        assertEquals(listOf("content://media/2"), vm.state.value.found.getValue(TagGap.Genre).map { it.uri })
    }

    @Test
    fun semEditarNaoLeNada() {
        val tags = ScanTags(mutableMapOf("content://media/1" to emptySet()))
        val vm = viewModel(tags)
        vm.scan()
        tags.scanned.clear()
        vm.refreshEdited()
        assertTrue(tags.scanned.isEmpty())
    }
}
