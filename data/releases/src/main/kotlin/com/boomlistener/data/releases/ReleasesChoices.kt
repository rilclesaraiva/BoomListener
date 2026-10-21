// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.releases

import android.content.Context

/**
 * O que ficou escolhido na tela de Lançamentos (modo de ver, filtro e janela
 * de tempo), pelo nome de cada opção; null = nunca escolheu (vale o padrão).
 */
data class ReleasesChoices(
    val view: String? = null,
    val filter: String? = null,
    val period: String? = null,
)

/** Guarda as escolhas dos Lançamentos, para a tela abrir como você deixou. */
interface ReleasesChoicesRepository {
    fun load(): ReleasesChoices
    fun save(choices: ReleasesChoices)
}

internal class PrefsReleasesChoices(context: Context) : ReleasesChoicesRepository {
    private val prefs = context.getSharedPreferences("releases_choices", Context.MODE_PRIVATE)

    override fun load() = ReleasesChoices(
        view = prefs.getString(KEY_VIEW, null),
        filter = prefs.getString(KEY_FILTER, null),
        period = prefs.getString(KEY_PERIOD, null),
    )

    override fun save(choices: ReleasesChoices) {
        prefs.edit()
            .putString(KEY_VIEW, choices.view)
            .putString(KEY_FILTER, choices.filter)
            .putString(KEY_PERIOD, choices.period)
            .apply()
    }

    private companion object {
        const val KEY_VIEW = "view"
        const val KEY_FILTER = "filter"
        const val KEY_PERIOD = "period"
    }
}

/** Escolhas que não saem da memória (testes e prévias). */
class MemoryReleasesChoices(private var current: ReleasesChoices = ReleasesChoices()) : ReleasesChoicesRepository {
    override fun load() = current
    override fun save(choices: ReleasesChoices) {
        current = choices
    }
}
