// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.boomlistener.core.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** As escolhas do usuário nos Ajustes. Ficam só no aparelho. */
data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = true,
    /** Desfoca a tela de trás quando outra entra por cima (telas e o player). */
    val backgroundBlur: Boolean = true,
)

interface UserPreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setBackgroundBlur(enabled: Boolean)
}

private val Context.userPreferencesStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

private object Keys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    val BACKGROUND_BLUR = booleanPreferencesKey("background_blur")
}

internal class DataStoreUserPreferencesRepository(context: Context) : UserPreferencesRepository {
    private val store = context.userPreferencesStore

    override val preferences: Flow<UserPreferences> = store.data.map { prefs ->
        UserPreferences(
            themeMode = parseThemeMode(prefs[Keys.THEME_MODE]),
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true,
            backgroundBlur = prefs[Keys.BACKGROUND_BLUR] ?: true,
        )
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[Keys.THEME_MODE] = mode.name }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        store.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    override suspend fun setBackgroundBlur(enabled: Boolean) {
        store.edit { it[Keys.BACKGROUND_BLUR] = enabled }
    }
}

/** Valor salvo → tema. Valor desconhecido (de uma versão futura, por exemplo) vira "Sistema". */
internal fun parseThemeMode(saved: String?): ThemeMode =
    ThemeMode.entries.firstOrNull { it.name == saved } ?: ThemeMode.System

/** Ponto de acesso provisório enquanto o app não tem Hilt. */
object PreferencesData {
    @Volatile
    private var instance: UserPreferencesRepository? = null

    fun repository(context: Context): UserPreferencesRepository =
        instance ?: synchronized(this) {
            instance ?: DataStoreUserPreferencesRepository(context.applicationContext).also { instance = it }
        }
}
