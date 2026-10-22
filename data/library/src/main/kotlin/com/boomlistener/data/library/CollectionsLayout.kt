// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.library

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Pílulas da Biblioteca que abrem em grade quando a pessoa nunca escolheu. */
val DEFAULT_GRID_KINDS: Set<String> = setOf("albums")

/** Liga ou desliga a grade de [kind], sem mexer nas outras pílulas. */
fun Set<String>.withGrid(kind: String, grid: Boolean): Set<String> = if (grid) this + kind else this - kind

/**
 * Como cada pílula da Biblioteca (gêneros, artistas, playlists, álbuns) é
 * mostrada: em grade ou em lista. O app lembra a escolha de cada uma.
 */
interface CollectionsLayoutRepository {
    /** Pílulas mostradas em grade (pelo nome); as outras ficam em lista. */
    val gridKinds: StateFlow<Set<String>>
    fun setGrid(kind: String, grid: Boolean)
}

internal class PrefsCollectionsLayout(context: Context) : CollectionsLayoutRepository {
    private val prefs = context.getSharedPreferences("collections_layout", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(prefs.getStringSet(KEY_GRID, null)?.toSet() ?: DEFAULT_GRID_KINDS)
    override val gridKinds: StateFlow<Set<String>> = state.asStateFlow()

    override fun setGrid(kind: String, grid: Boolean) {
        val next = state.value.withGrid(kind, grid)
        state.value = next
        prefs.edit().putStringSet(KEY_GRID, next).apply()
    }

    private companion object {
        const val KEY_GRID = "grid_kinds"
    }
}

/** Escolhas que não saem da memória (testes e prévias). */
class MemoryCollectionsLayout(initial: Set<String> = DEFAULT_GRID_KINDS) : CollectionsLayoutRepository {
    private val state = MutableStateFlow(initial)
    override val gridKinds: StateFlow<Set<String>> = state.asStateFlow()
    override fun setGrid(kind: String, grid: Boolean) {
        state.value = state.value.withGrid(kind, grid)
    }
}

object CollectionsLayoutData {
    @Volatile private var instance: CollectionsLayoutRepository? = null

    fun repository(context: Context): CollectionsLayoutRepository =
        instance ?: synchronized(this) {
            instance ?: PrefsCollectionsLayout(context.applicationContext).also { instance = it }
        }
}
