// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.settings

import com.boomlistener.core.datastore.UserPreferences
import com.boomlistener.core.datastore.UserPreferencesRepository
import com.boomlistener.core.model.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

private class FakePreferences : UserPreferencesRepository {
    override val preferences = MutableStateFlow(UserPreferences())
    override suspend fun setThemeMode(mode: ThemeMode) = preferences.update { it.copy(themeMode = mode) }
    override suspend fun setDynamicColor(enabled: Boolean) = preferences.update { it.copy(dynamicColor = enabled) }
    override suspend fun setBackgroundBlur(enabled: Boolean) = preferences.update { it.copy(backgroundBlur = enabled) }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppearanceViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun trocaTemaECorDinamica() = runTest(dispatcher) {
        val viewModel = AppearanceViewModel(FakePreferences())
        backgroundScope.launch(dispatcher) { viewModel.preferences.collect {} }

        viewModel.setThemeMode(ThemeMode.Amoled)
        viewModel.setDynamicColor(false)

        assertEquals(ThemeMode.Amoled, viewModel.preferences.value.themeMode)
        assertFalse(viewModel.preferences.value.dynamicColor)
    }

    @Test
    fun desfoqueLigadoPorPadraoEDesligavel() = runTest(dispatcher) {
        val viewModel = AppearanceViewModel(FakePreferences())
        backgroundScope.launch(dispatcher) { viewModel.preferences.collect {} }

        assertEquals(true, viewModel.preferences.value.backgroundBlur)
        viewModel.setBackgroundBlur(false)
        assertFalse(viewModel.preferences.value.backgroundBlur)
    }
}
