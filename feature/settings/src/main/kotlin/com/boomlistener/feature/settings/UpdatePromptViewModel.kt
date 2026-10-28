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
 * O aviso de versão nova que aparece sozinho ao abrir o app. Abrindo do zero,
 * sempre consulta o GitHub; voltando ao app, no máximo a cada 10 min. Respeita
 * o adiar (sino) e o "pular esta versão"; "Agora não" vale só até o app ser
 * aberto de novo.
 */
internal class UpdatePromptViewModel(
    private val repository: UpdatesRepository,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _available = MutableStateFlow<UpdateCheckResult.Available?>(null)
    val available: StateFlow<UpdateCheckResult.Available?> = _available.asStateFlow()

    // A pessoa já respondeu ao aviso nesta sessão (ex.: foi baixar e voltou):
    // não reaparece até o app ser aberto de novo.
    private var handled = false
    private var checking = false

    init {
        checkOnOpen(coldStart = true)
    }

    /**
     * Ao abrir o app (e ao voltar para ele): mostra na hora a versão nova já
     * conhecida e, passado o intervalo, consulta o GitHub de novo. Antes, a
     * consulta só acontecia na criação da tela e a hora era marcada antes de
     * saber a resposta: uma consulta que não achava nada (ou falhava sem
     * internet) escondia por horas uma versão publicada logo depois.
     */
    fun checkOnOpen(coldStart: Boolean = false) {
        if (handled) return
        repository.cachedUpdate()?.let { cached ->
            if (shouldPrompt(cached.latestVersion, now(), repository.promptState())) _available.value = cached
        }
        if (checking || !shouldAutoCheck(now(), repository.promptState(), coldStart)) return
        checking = true
        viewModelScope.launch {
            val result = repository.checkForUpdates()
            // Só conta como consultado quando o GitHub respondeu.
            if (result !is UpdateCheckResult.Failed) repository.markAutoCheck(now())
            // A resposta nova sempre vence a guardada: o aviso mostra a versão
            // mais recente e as notas dela (nunca as de uma Release antiga).
            when {
                handled -> Unit
                result is UpdateCheckResult.Available &&
                    shouldPrompt(result.latestVersion, now(), repository.promptState()) -> _available.value = result
                result is UpdateCheckResult.UpToDate || result is UpdateCheckResult.NoReleases -> _available.value = null
                else -> Unit
            }
            checking = false
        }
    }

    /** Foi baixar: some agora (volta numa próxima abertura se ainda não atualizou). */
    fun onDownload() {
        handled = true
        _available.value = null
    }

    /**
     * "Agora não": some só agora. Fechar o app (tirar dos recentes) e abrir de
     * novo mostra o aviso outra vez; para silenciar por mais tempo há o sino.
     */
    fun later() {
        handled = true
        _available.value = null
    }

    fun snooze(period: SnoozePeriod) {
        handled = true
        repository.snoozePrompt(now() + period.days * DAY_MS)
        _available.value = null
    }

    fun skipVersion() {
        handled = true
        _available.value?.let { repository.skipVersion(it.latestVersion) }
        _available.value = null
    }
}
