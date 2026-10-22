// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.settings

import com.boomlistener.data.updates.AUTO_CHECK_INTERVAL_MS
import com.boomlistener.data.updates.UpdateCheckResult
import com.boomlistener.data.updates.UpdatePromptState
import com.boomlistener.data.updates.UpdatesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

private const val NOW = 1_000_000_000_000L
private const val DAY = 24 * 60 * 60 * 1000L

private class PromptFake(
    private val result: UpdateCheckResult,
    var state: UpdatePromptState = UpdatePromptState(),
    var cached: UpdateCheckResult.Available? = null,
) : UpdatesRepository {
    override fun cachedUpdate() = cached
    var checks = 0
    override suspend fun checkForUpdates(): UpdateCheckResult {
        checks++
        return result
    }
    override fun promptState() = state
    override fun snoozePrompt(untilMs: Long) { state = state.copy(snoozedUntilMs = untilMs) }
    override fun skipVersion(version: String) { state = state.copy(skippedVersion = version) }
    override fun markAutoCheck(atMs: Long) { state = state.copy(lastAutoCheckMs = atMs) }
}

@OptIn(ExperimentalCoroutinesApi::class)
class UpdatePromptViewModelTest {
    private val available = UpdateCheckResult.Available("0.21.0", "0.22.0", "https://example.com", null, notes = "### New")

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun versaoNovaApareceSozinha() {
        val fake = PromptFake(available)
        val viewModel = UpdatePromptViewModel(fake) { NOW }
        assertEquals(available, viewModel.available.value)
        assertEquals(NOW, fake.state.lastAutoCheckMs)
    }

    @Test
    fun semVersaoNovaNaoAparece() {
        val viewModel = UpdatePromptViewModel(PromptFake(UpdateCheckResult.UpToDate("0.22.0"))) { NOW }
        assertNull(viewModel.available.value)
    }

    @Test
    fun naoConsultaDeNovoLogoDepois() {
        val fake = PromptFake(available, UpdatePromptState(lastAutoCheckMs = NOW - 1_000))
        val viewModel = UpdatePromptViewModel(fake) { NOW }
        assertEquals(0, fake.checks)
        assertNull(viewModel.available.value)
        UpdatePromptViewModel(fake) { NOW + AUTO_CHECK_INTERVAL_MS }
        assertEquals(1, fake.checks)
    }

    @Test
    fun agoraNaoAdiaUmDia() {
        val fake = PromptFake(available)
        val viewModel = UpdatePromptViewModel(fake) { NOW }
        viewModel.later()
        assertNull(viewModel.available.value)
        assertEquals(NOW + DAY, fake.state.snoozedUntilMs)
    }

    @Test
    fun sinoSilenciaPeloPeriodoEscolhido() {
        val fake = PromptFake(available)
        val viewModel = UpdatePromptViewModel(fake) { NOW }
        viewModel.snooze(SnoozePeriod.Month)
        assertEquals(NOW + 30 * DAY, fake.state.snoozedUntilMs)
        // Dentro do prazo, nem consulta.
        val again = UpdatePromptViewModel(fake) { NOW + AUTO_CHECK_INTERVAL_MS }
        assertNull(again.available.value)
    }

    @Test
    fun pularVersaoSoVoltaNaProxima() {
        val fake = PromptFake(available)
        UpdatePromptViewModel(fake) { NOW }.skipVersion()
        assertEquals("0.22.0", fake.state.skippedVersion)
        val later = UpdatePromptViewModel(fake) { NOW + AUTO_CHECK_INTERVAL_MS }
        assertNull(later.available.value)

        val newer = PromptFake(UpdateCheckResult.Available("0.21.0", "0.23.0", "https://example.com", null), fake.state)
        assertNotNull(UpdatePromptViewModel(newer) { NOW + 2 * AUTO_CHECK_INTERVAL_MS }.available.value)
    }

    @Test
    fun versaoJaConhecidaApareceSemEsperarOIntervalo() {
        // Consultou há pouco (dentro do intervalo), mas a versão nova já era conhecida.
        val fake = PromptFake(available, UpdatePromptState(lastAutoCheckMs = NOW - 1_000), cached = available)
        val viewModel = UpdatePromptViewModel(fake) { NOW }
        assertEquals(available, viewModel.available.value)
        assertEquals(0, fake.checks)
    }

    @Test
    fun respostaNovaDoGitHubTrocaAGuardada() {
        // Guardada: 0.22.0. O GitHub agora responde 0.23.0: o aviso mostra a 0.23.0 (e as notas dela).
        val newer = UpdateCheckResult.Available("0.21.0", "0.23.0", "https://example.com", null, notes = "### Newer")
        val fake = PromptFake(newer, cached = available)
        val viewModel = UpdatePromptViewModel(fake) { NOW }
        assertEquals(newer, viewModel.available.value)
    }

    @Test
    fun falhaSemInternetNaoContaComoConsultado() {
        val fake = PromptFake(UpdateCheckResult.Failed)
        UpdatePromptViewModel(fake) { NOW }
        assertEquals(0L, fake.state.lastAutoCheckMs)
    }

    @Test
    fun depoisDeResponderNaoReapareceAoVoltarParaOApp() {
        val fake = PromptFake(available, cached = available)
        val viewModel = UpdatePromptViewModel(fake) { NOW }
        viewModel.onDownload()
        viewModel.checkOnOpen()
        assertNull(viewModel.available.value)
    }
}
