// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.datastore

import com.boomlistener.core.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class UserPreferencesTest {
    @Test
    fun temaSalvoVoltaIgual() {
        ThemeMode.entries.forEach { assertEquals(it, parseThemeMode(it.name)) }
    }

    @Test
    fun temaDesconhecidoViraSistema() {
        assertEquals(ThemeMode.System, parseThemeMode(null))
        assertEquals(ThemeMode.System, parseThemeMode("Neon"))
    }
}
