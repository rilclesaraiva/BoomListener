// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boomlistener.data.updates.UpdateCheckResult
import com.boomlistener.data.updates.UpdatesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal sealed interface UpdatesUiState {
    data object Idle : UpdatesUiState
    data object Checking : UpdatesUiState
    data class Done(val result: UpdateCheckResult) : UpdatesUiState
}

/** "Verificar atualizações": só consulta o GitHub quando o usuário toca. */
internal class UpdatesViewModel(
    private val repository: UpdatesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UpdatesUiState>(UpdatesUiState.Idle)
    val state: StateFlow<UpdatesUiState> = _state.asStateFlow()

    /** Mostra o aviso de versão nova; some ao fechar e volta a cada nova verificação. */
    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    fun check() {
        if (_state.value == UpdatesUiState.Checking) return
        _state.value = UpdatesUiState.Checking
        viewModelScope.launch {
            val result = repository.checkForUpdates()
            _state.value = UpdatesUiState.Done(result)
            _showDialog.value = result is UpdateCheckResult.Available
        }
    }

    fun dismissDialog() {
        _showDialog.value = false
    }

    /** Adiar os avisos automáticos (o botão de verificar continua funcionando). */
    fun snooze(period: SnoozePeriod) {
        repository.snoozePrompt(System.currentTimeMillis() + period.days * 24 * 60 * 60 * 1000L)
        _showDialog.value = false
    }

    fun skipVersion() {
        ((_state.value as? UpdatesUiState.Done)?.result as? UpdateCheckResult.Available)?.let {
            repository.skipVersion(it.latestVersion)
        }
        _showDialog.value = false
    }
}
