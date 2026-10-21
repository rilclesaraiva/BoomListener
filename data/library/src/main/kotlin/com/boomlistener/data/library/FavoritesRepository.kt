// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.library

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File

/**
 * As músicas favoritas (o coração): só a lista de endereços, a mais nova
 * primeiro. Guardada em `filesDir/favorites/favorites.json`, no aparelho.
 */
interface FavoritesRepository {
    /** Endereços das favoritas, a favoritada por último primeiro. */
    val favorites: StateFlow<List<String>>

    /** Liga ou desliga o coração de [uri]. Devolve se ficou favorita. */
    suspend fun toggle(uri: String): Boolean
}

/** Liga/desliga [uri]: entra no começo (a mais nova primeiro) ou sai. */
internal fun toggleFavorite(current: List<String>, uri: String): List<String> =
    if (uri in current) current - uri else listOf(uri) + current

internal class FileFavoritesRepository(context: Context) : FavoritesRepository {
    private val file = File(context.filesDir, "favorites/favorites.json")
    private val mutex = Mutex()
    private val state = MutableStateFlow<List<String>>(emptyList())
    override val favorites: StateFlow<List<String>> = state.asStateFlow()

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            mutex.withLock { state.value = runCatching { read() }.getOrDefault(emptyList()) }
        }
    }

    private fun read(): List<String> {
        if (!file.exists()) return emptyList()
        val array = JSONArray(file.readText())
        return (0 until array.length()).map { array.getString(it) }.distinct()
    }

    override suspend fun toggle(uri: String): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val updated = toggleFavorite(state.value, uri)
            state.value = updated
            runCatching {
                file.parentFile?.mkdirs()
                val temp = File(file.parentFile, file.name + ".tmp")
                temp.writeText(JSONArray(updated).toString())
                if (!temp.renameTo(file)) {
                    file.writeText(temp.readText())
                    temp.delete()
                }
            }
            uri in updated
        }
    }
}

/** Ponto de acesso provisório enquanto o app não tem Hilt. */
object FavoritesData {
    @Volatile
    private var instance: FavoritesRepository? = null

    fun repository(context: Context): FavoritesRepository =
        instance ?: synchronized(this) {
            instance ?: FileFavoritesRepository(context.applicationContext).also { instance = it }
        }
}
