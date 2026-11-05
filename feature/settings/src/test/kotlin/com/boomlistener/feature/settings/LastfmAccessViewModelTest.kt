// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.settings

import com.boomlistener.data.lastfm.KeyCheck
import com.boomlistener.data.lastfm.LastfmKeyStore
import com.boomlistener.data.lastfm.LastfmRepository
import com.boomlistener.data.lastfm.BioResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val KEY = "0123456789abcdef0123456789abcdef"

private class FakeStore(initial: String? = null, private val canWrite: Boolean = true) : LastfmKeyStore {
    private val state = MutableStateFlow(initial)
    private val rejectedState = MutableStateFlow(false)
    override val ownKey: StateFlow<String?> = state
    override val rejected: StateFlow<Boolean> = rejectedState

    override fun setOwnKey(key: String?): Boolean {
        if (!canWrite) return false
        state.value = key
        rejectedState.value = false
        return true
    }

    override fun markRejected() {
        rejectedState.value = true
    }
}

private class FakeLastfm(private val check: KeyCheck) : LastfmRepository {
    var tested: String? = null
    override suspend fun artistBio(artistName: String, language: String) = BioResult.NotFound
    override suspend fun checkKey(key: String): KeyCheck {
        tested = key
        return check
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LastfmAccessViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun comecaNoServidorDoBoomSemChave() {
        val viewModel = LastfmAccessViewModel(FakeStore(), FakeLastfm(KeyCheck.Valid))
        assertFalse(viewModel.state.value.usingOwnKey)
        assertNull(viewModel.state.value.savedKeyEnd)
    }

    @Test
    fun comChaveSalvaMostraSoOFimDela() {
        val viewModel = LastfmAccessViewModel(FakeStore(KEY), FakeLastfm(KeyCheck.Valid))
        assertTrue(viewModel.state.value.usingOwnKey)
        assertEquals("cdef", viewModel.state.value.savedKeyEnd)
        assertEquals("", viewModel.state.value.input)
    }

    @Test
    fun chaveAceitaPeloLastfmFicaSalva() {
        val store = FakeStore()
        val lastfm = FakeLastfm(KeyCheck.Valid)
        val viewModel = LastfmAccessViewModel(store, lastfm)

        viewModel.onInput("  ${KEY.uppercase()} ")
        viewModel.save()

        assertEquals(KEY, lastfm.tested)
        assertEquals(KEY, store.ownKey.value)
        val state = viewModel.state.value
        assertTrue(state.usingOwnKey)
        assertEquals(LastfmAccessMessage.Saved, state.message)
        assertEquals("", state.input)
        assertFalse(state.checking)
    }

    @Test
    fun chaveRecusadaNaoEhSalva() {
        val store = FakeStore()
        val viewModel = LastfmAccessViewModel(store, FakeLastfm(KeyCheck.Invalid))

        viewModel.onInput(KEY)
        viewModel.save()

        assertNull(store.ownKey.value)
        assertEquals(LastfmAccessMessage.Rejected, viewModel.state.value.message)
        assertFalse(viewModel.state.value.usingOwnKey)
        assertFalse(viewModel.state.value.checking)
        assertEquals(KEY, viewModel.state.value.input)
    }

    @Test
    fun semInternetNaoSalvaENemDizQueEstaErrada() {
        val store = FakeStore()
        val viewModel = LastfmAccessViewModel(store, FakeLastfm(KeyCheck.Failed))

        viewModel.onInput(KEY)
        viewModel.save()

        assertNull(store.ownKey.value)
        assertEquals(LastfmAccessMessage.CheckFailed, viewModel.state.value.message)
    }

    @Test
    fun textoQueNaoPareceChaveNemChegaAoLastfm() {
        val lastfm = FakeLastfm(KeyCheck.Valid)
        val viewModel = LastfmAccessViewModel(FakeStore(), lastfm)

        viewModel.onInput("minha chave")
        viewModel.save()

        assertNull(lastfm.tested)
        assertEquals(LastfmAccessMessage.BadFormat, viewModel.state.value.message)
    }

    @Test
    fun digitarLimpaOAvisoAnterior() {
        val viewModel = LastfmAccessViewModel(FakeStore(), FakeLastfm(KeyCheck.Invalid))
        viewModel.onInput(KEY)
        viewModel.save()
        assertEquals(LastfmAccessMessage.Rejected, viewModel.state.value.message)

        viewModel.onInput(KEY.dropLast(1))
        assertNull(viewModel.state.value.message)
    }

    @Test
    fun voltarAoServidorDoBoomEsqueceAChave() {
        val store = FakeStore(KEY)
        val viewModel = LastfmAccessViewModel(store, FakeLastfm(KeyCheck.Valid))

        viewModel.useBoomServer()

        assertNull(store.ownKey.value)
        assertFalse(viewModel.state.value.usingOwnKey)
        assertNull(viewModel.state.value.savedKeyEnd)
    }

    @Test
    fun discoCheioNaoDerrubaNemMarcaComoSalva() {
        val store = FakeStore(canWrite = false)
        val viewModel = LastfmAccessViewModel(store, FakeLastfm(KeyCheck.Valid))

        viewModel.onInput(KEY)
        viewModel.save()

        assertNull(store.ownKey.value)
        val state = viewModel.state.value
        assertEquals(LastfmAccessMessage.SaveFailed, state.message)
        assertFalse(state.usingOwnKey)
        assertFalse(state.checking)
    }

    @Test
    fun chaveSalvaQueOLastfmPassouARecusarAvisaAPessoa() {
        val store = FakeStore(KEY)
        val viewModel = LastfmAccessViewModel(store, FakeLastfm(KeyCheck.Valid))
        assertFalse(viewModel.state.value.keyRejected)

        store.markRejected()

        assertTrue(viewModel.state.value.keyRejected)
        assertTrue(viewModel.state.value.usingOwnKey)
    }

    @Test
    fun trocarAChaveOuVoltarAoServidorTiraOAviso() {
        val store = FakeStore(KEY)
        val viewModel = LastfmAccessViewModel(store, FakeLastfm(KeyCheck.Valid))
        store.markRejected()
        assertTrue(viewModel.state.value.keyRejected)

        viewModel.onInput("fedcba9876543210fedcba9876543210")
        viewModel.save()
        assertFalse(viewModel.state.value.keyRejected)

        store.markRejected()
        viewModel.useBoomServer()
        assertFalse(viewModel.state.value.keyRejected)
        assertFalse(viewModel.state.value.usingOwnKey)
    }

    @Test
    fun naoDaParaApagarAChaveMantemOEstadoAtual() {
        val store = FakeStore(KEY, canWrite = false)
        val viewModel = LastfmAccessViewModel(store, FakeLastfm(KeyCheck.Valid))

        viewModel.useBoomServer()

        assertTrue(viewModel.state.value.usingOwnKey)
        assertEquals(KEY, store.ownKey.value)
    }
}
