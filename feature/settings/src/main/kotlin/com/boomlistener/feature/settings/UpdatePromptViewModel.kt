// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boomlistener.data.updates.UpdateCheckResult
import com.boomlistener.data.updates.UpdatesRepository
import com.boomlistener.data.updates.shouldAutoCheck
import com.boomlistener.data.updates.shouldPrompt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val DAY_MS = 24 * 60 * 60 * 1000L

/** Por quanto tempo o sino cortado silencia os avisos. */
internal enum class SnoozePeriod(val days: Int) { Week(7), Month(30) }

/**
 * O aviso de versão nova que aparece sozinho ao abrir o app. Consulta o
 * GitHub no máximo a cada 6 h e respeita o "adiar" e o "pular esta versão".
 */
internal class UpdatePromptViewModel(
    private val repository: UpdatesRepository,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _available = MutableStateFlow<UpdateCheckResult.Available?>(null)
    val available: StateFlow<UpdateCheckResult.Available?> = _available.asStateFlow()

    init {
        val state = repository.promptState()
        if (shouldAutoCheck(now(), state)) {
            viewModelScope.launch {
                repository.markAutoCheck(now())
                val result = repository.checkForUpdates()
                if (result is UpdateCheckResult.Available && shouldPrompt(result.latestVersion, now(), repository.promptState())) {
                    _available.value = result
                }
            }
        }
    }

    /** Foi baixar: some agora (volta numa próxima abertura se ainda não atualizou). */
    fun onDownload() {
        _available.value = null
    }

    /** "Agora não": lembra de novo amanhã. */
    fun later() {
        repository.snoozePrompt(now() + DAY_MS)
        _available.value = null
    }

    fun snooze(period: SnoozePeriod) {
        repository.snoozePrompt(now() + period.days * DAY_MS)
        _available.value = null
    }

    fun skipVersion() {
        _available.value?.let { repository.skipVersion(it.latestVersion) }
        _available.value = null
    }
}
