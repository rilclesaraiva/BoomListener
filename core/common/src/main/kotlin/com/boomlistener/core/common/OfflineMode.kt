// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.common

import android.content.Context
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Modo 100% offline (Ajustes → Modo offline; desligado por padrão). Ligado,
 * nenhuma consulta vai para a internet (Deezer, Last.fm, MusicBrainz, letras,
 * imagens, radar de lançamentos). Só a verificação de atualizações continua.
 * O que já foi baixado antes (capas e fotos em cache) continua aparecendo.
 *
 * Cada lugar que abre uma conexão chama [requireOnline] antes: ligado, ela
 * falha como uma falta de internet, e as telas já sabem lidar com isso.
 */
object OfflineMode {
    private const val PREFS = "offline_mode"
    private const val KEY = "enabled"

    private val state = MutableStateFlow(false)

    @Volatile
    private var loaded = false

    /** Lê a escolha guardada (chamado ao abrir o app). */
    fun init(context: Context) {
        if (loaded) return
        loaded = true
        state.value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false)
    }

    val enabled: StateFlow<Boolean>
        get() = state.asStateFlow()

    val isOn: Boolean
        get() = state.value

    fun set(context: Context, on: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, on).apply()
        loaded = true
        state.value = on
    }

    /** Antes de abrir uma conexão: com o modo offline ligado, falha aqui mesmo. */
    @Throws(OfflineModeException::class)
    fun requireOnline() {
        if (state.value) throw OfflineModeException()
    }
}

/** A consulta não foi feita porque o modo offline está ligado. */
class OfflineModeException : IOException("Offline mode is on")
