// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boomlistener.data.lastfm.KeyCheck
import com.boomlistener.data.lastfm.LastfmKeyStore
import com.boomlistener.data.lastfm.LastfmRepository
import com.boomlistener.data.lastfm.normalizeLastfmKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** O que dizer depois de tocar em "Testar e salvar". */
internal enum class LastfmAccessMessage { Saved, BadFormat, Rejected, CheckFailed, SaveFailed }

internal data class LastfmAccessUiState(
    /** Há uma chave própria salva: o app fala direto com o Last.fm. */
    val usingOwnKey: Boolean = false,
    /** Fim da chave salva, para a pessoa reconhecer qual é (a chave inteira nunca aparece). */
    val savedKeyEnd: String? = null,
    val input: String = "",
    val checking: Boolean = false,
    val message: LastfmAccessMessage? = null,
    /** O Last.fm deixou de aceitar a chave salva: só aparece até a pessoa trocá-la ou voltar ao servidor do Boom. */
    val keyRejected: Boolean = false,
)

/**
 * Ajustes → Sistema → Last.fm: usar o servidor do Boom (padrão) ou a chave
 * própria da pessoa. A chave só é salva depois que o Last.fm a aceita.
 */
internal class LastfmAccessViewModel(
    private val store: LastfmKeyStore,
    private val repository: LastfmRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(stateFor(store.ownKey.value))
    val state: StateFlow<LastfmAccessUiState> = combine(_state, store.rejected) { state, rejected ->
        state.copy(keyRejected = rejected && state.usingOwnKey)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, _state.value)

    fun onInput(text: String) {
        if (_state.value.checking) return
        _state.value = _state.value.copy(input = text.trim(), message = null)
    }

    /** Testa a chave digitada no Last.fm e, se ele aceitar, passa a usá-la. */
    fun save() {
        val current = _state.value
        if (current.checking) return
        val key = normalizeLastfmKey(current.input)
        if (key == null) {
            _state.value = current.copy(message = LastfmAccessMessage.BadFormat)
            return
        }
        _state.value = current.copy(checking = true, message = null)
        viewModelScope.launch {
            when (repository.checkKey(key)) {
                KeyCheck.Valid ->
                    _state.value = if (store.setOwnKey(key)) {
                        stateFor(key).copy(message = LastfmAccessMessage.Saved)
                    } else {
                        _state.value.copy(checking = false, message = LastfmAccessMessage.SaveFailed)
                    }
                KeyCheck.Invalid ->
                    _state.value = _state.value.copy(checking = false, message = LastfmAccessMessage.Rejected)
                KeyCheck.Failed ->
                    _state.value = _state.value.copy(checking = false, message = LastfmAccessMessage.CheckFailed)
            }
        }
    }

    /** Volta ao servidor do Boom e esquece a chave. */
    fun useBoomServer() {
        if (store.setOwnKey(null)) _state.value = stateFor(null)
    }

    private fun stateFor(key: String?) = LastfmAccessUiState(
        usingOwnKey = key != null,
        savedKeyEnd = key?.takeLast(4),
    )
}
