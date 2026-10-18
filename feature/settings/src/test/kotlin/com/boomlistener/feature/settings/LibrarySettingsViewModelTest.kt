// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.settings

import android.graphics.Bitmap
import com.boomlistener.core.model.Track
import com.boomlistener.data.library.LibraryFilter
import com.boomlistener.data.library.LibraryRepository
import com.boomlistener.data.library.MusicFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val ROCK = "/storage/emulated/0/Music/Rock"
private const val DOWNLOAD = "/storage/emulated/0/Download"

private class FakeLibrary : LibraryRepository {
    override val filter = MutableStateFlow(LibraryFilter())
    override val version = MutableStateFlow(0)
    var folderList = listOf(
        MusicFolder(DOWNLOAD, "Download", "Download", 3),
        MusicFolder(ROCK, "Rock", "Music/Rock", 10),
    )
    var saves = 0

    override suspend fun getTracks(): List<Track> = emptyList()
    override suspend fun loadArtwork(trackUri: String, sizePx: Int): Bitmap? = null
    override suspend fun folders() = folderList
    override fun setFilter(filter: LibraryFilter) {
        saves++
        this.filter.value = filter
        version.value++
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LibrarySettingsViewModelTest {

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun carregaAsPastas() {
        val viewModel = LibrarySettingsViewModel(FakeLibrary())
        assertEquals(2, viewModel.state.value.folders?.size)
        assertFalse(viewModel.state.value.filter.onlyChosenFolders)
    }

    @Test
    fun duracaoMinimaFicaNoLimiteDaBarra() {
        val library = FakeLibrary()
        val viewModel = LibrarySettingsViewModel(library)
        viewModel.setMinDuration(500)
        assertEquals(LibraryFilter.MAX_MIN_DURATION_SECONDS, library.filter.value.minDurationSeconds)
        viewModel.setMinDuration(-3)
        assertEquals(0, library.filter.value.minDurationSeconds)
    }

    @Test
    fun mesmaDuracaoNaoRecarregaABiblioteca() {
        val library = FakeLibrary()
        val viewModel = LibrarySettingsViewModel(library)
        viewModel.setMinDuration(30)
        viewModel.setMinDuration(30)
        assertEquals(1, library.saves)
    }

    @Test
    fun escolherPastasComecaComTodasMarcadas() {
        val library = FakeLibrary()
        val viewModel = LibrarySettingsViewModel(library)
        viewModel.useChosenFolders()
        assertTrue(library.filter.value.onlyChosenFolders)
        assertEquals(setOf(ROCK, DOWNLOAD), library.filter.value.folders)
    }

    @Test
    fun desmarcarTiraSoAquelaPasta() {
        val library = FakeLibrary()
        val viewModel = LibrarySettingsViewModel(library)
        viewModel.useChosenFolders()
        viewModel.toggleFolder(DOWNLOAD)
        assertEquals(setOf(ROCK), library.filter.value.folders)
        viewModel.toggleFolder(DOWNLOAD)
        assertEquals(setOf(ROCK, DOWNLOAD), library.filter.value.folders)
    }

    @Test
    fun voltarParaTodasGuardaAEscolha() {
        val library = FakeLibrary()
        val viewModel = LibrarySettingsViewModel(library)
        viewModel.useChosenFolders()
        viewModel.toggleFolder(DOWNLOAD)
        viewModel.useAllFolders()
        assertFalse(library.filter.value.onlyChosenFolders)
        viewModel.useChosenFolders()
        assertEquals(setOf(ROCK), library.filter.value.folders)
    }

    @Test
    fun pastaNovaApareceSemReabrir() {
        val library = FakeLibrary()
        val viewModel = LibrarySettingsViewModel(library)
        library.folderList = library.folderList + MusicFolder("/storage/emulated/0/Music/Jazz", "Jazz", "Music/Jazz", 1)
        library.version.value++
        assertEquals(3, viewModel.state.value.folders?.size)
    }
}
