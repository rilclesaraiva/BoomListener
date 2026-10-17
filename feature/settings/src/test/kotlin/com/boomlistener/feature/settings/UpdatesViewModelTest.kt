// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.settings

import com.boomlistener.data.updates.UpdateCheckResult
import com.boomlistener.data.updates.UpdatesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeUpdates(private val result: UpdateCheckResult) : UpdatesRepository {
    override suspend fun checkForUpdates() = result
}

@OptIn(ExperimentalCoroutinesApi::class)
class UpdatesViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun versaoNovaAbreOAviso() {
        val available = UpdateCheckResult.Available("0.5.0", "0.6.0", "https://example.com", null)
        val viewModel = UpdatesViewModel(FakeUpdates(available))
        assertEquals(UpdatesUiState.Idle, viewModel.state.value)

        viewModel.check()
        assertEquals(UpdatesUiState.Done(available), viewModel.state.value)
        assertTrue(viewModel.showDialog.value)

        viewModel.dismissDialog()
        assertFalse(viewModel.showDialog.value)
    }

    @Test
    fun semReleaseNaoAbreAviso() {
        val viewModel = UpdatesViewModel(FakeUpdates(UpdateCheckResult.NoReleases))
        viewModel.check()
        assertEquals(UpdatesUiState.Done(UpdateCheckResult.NoReleases), viewModel.state.value)
        assertFalse(viewModel.showDialog.value)
    }
}
