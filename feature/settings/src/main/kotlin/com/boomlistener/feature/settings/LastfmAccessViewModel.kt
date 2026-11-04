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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** O que dizer depois de tocar em "Testar e salvar". */
internal enum class LastfmAccessMessage { Saved, BadFormat, Rejected, CheckFailed }

internal data class LastfmAccessUiState(
    /** Há uma chave própria salva: o app fala direto com o Last.fm. */
    val usingOwnKey: Boolean = false,
    /** Fim da chave salva, para a pessoa reconhecer qual é (a chave inteira nunca aparece). */
    val savedKeyEnd: String? = null,
    val input: String = "",
    val checking: Boolean = false,
    val message: LastfmAccessMessage? = null,
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
    val state: StateFlow<LastfmAccessUiState> = _state.asStateFlow()

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
                KeyCheck.Valid -> {
                    store.setOwnKey(key)
                    _state.value = stateFor(key).copy(message = LastfmAccessMessage.Saved)
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
        store.setOwnKey(null)
        _state.value = stateFor(null)
    }

    private fun stateFor(key: String?) = LastfmAccessUiState(
        usingOwnKey = key != null,
        savedKeyEnd = key?.takeLast(4),
    )
}
