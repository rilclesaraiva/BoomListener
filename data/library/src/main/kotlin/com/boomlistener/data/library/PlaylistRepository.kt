// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.library

import android.content.Context
import com.boomlistener.core.model.Playlist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Playlists do app (ficam só no aparelho). */
interface PlaylistRepository {
    /** Mexidas por último primeiro. */
    val playlists: Flow<List<Playlist>>

    /** Cria e devolve o id. */
    suspend fun create(name: String, trackUris: List<String> = emptyList()): Long
    suspend fun rename(id: Long, name: String)
    suspend fun delete(id: Long)

    /** Acrescenta no fim, pulando as que já estão. Devolve quantas entraram. */
    suspend fun addTracks(id: Long, uris: List<String>): Int
    suspend fun removeAt(id: Long, index: Int)
    suspend fun move(id: Long, from: Int, to: Int)
}

internal class FilePlaylistRepository(context: Context) : PlaylistRepository {
    private val file = File(context.filesDir, "playlists/playlists.json").apply { parentFile?.mkdirs() }
    private val mutex = Mutex()
    private val state = MutableStateFlow<List<Playlist>?>(null)

    override val playlists: Flow<List<Playlist>> = state
        .onStart { ensureLoaded() }
        .filterNotNull()
        .map { list -> list.sortedByDescending { it.updatedAtMs } }

    private suspend fun ensureLoaded() {
        if (state.value != null) return
        withContext(Dispatchers.IO) {
            mutex.withLock {
                if (state.value == null) state.value = runCatching { readPlaylists(file) }.getOrDefault(emptyList())
            }
        }
    }

    /** Aplica [change] e grava. */
    private suspend fun <T> update(change: (List<Playlist>) -> Pair<List<Playlist>, T>): T {
        ensureLoaded()
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                val (updated, result) = change(state.value.orEmpty())
                state.value = updated
                runCatching { writePlaylists(file, updated) }
                result
            }
        }
    }

    override suspend fun create(name: String, trackUris: List<String>): Long = update { list ->
        val created = newPlaylist(list, name, trackUris, System.currentTimeMillis())
        (list + created) to created.id
    }

    override suspend fun rename(id: Long, name: String) = update { list ->
        list.edit(id) { it.copy(name = cleanName(name, it.name)) } to Unit
    }

    override suspend fun delete(id: Long) = update { list -> list.filterNot { it.id == id } to Unit }

    override suspend fun addTracks(id: Long, uris: List<String>): Int = update { list ->
        var added = 0
        val updated = list.edit(id) { playlist ->
            val (result, count) = addUnique(playlist.trackUris, uris)
            added = count
            playlist.copy(trackUris = result)
        }
        updated to added
    }

    override suspend fun removeAt(id: Long, index: Int) = update { list ->
        list.edit(id) { playlist ->
            playlist.copy(trackUris = playlist.trackUris.filterIndexed { i, _ -> i != index })
        } to Unit
    }

    override suspend fun move(id: Long, from: Int, to: Int) = update { list ->
        list.edit(id) { playlist -> playlist.copy(trackUris = moveItem(playlist.trackUris, from, to)) } to Unit
    }

    private fun List<Playlist>.edit(id: Long, change: (Playlist) -> Playlist): List<Playlist> {
        val now = System.currentTimeMillis()
        return map { if (it.id == id) change(it).copy(updatedAtMs = now) else it }
    }
}

/** Playlist nova com id único e nome limpo (vazio vira "Playlist 1", "Playlist 2"…). */
internal fun newPlaylist(existing: List<Playlist>, name: String, uris: List<String>, nowMs: Long): Playlist {
    val id = maxOf(nowMs, (existing.maxOfOrNull { it.id } ?: 0) + 1)
    val fallback = "Playlist ${existing.size + 1}"
    return Playlist(id, cleanName(name, fallback), addUnique(emptyList(), uris).first, nowMs, nowMs)
}

internal fun cleanName(name: String, fallback: String): String =
    name.trim().replace(Regex("\\s+"), " ").take(80).ifEmpty { fallback }

/** Acrescenta [new] no fim de [current] sem repetir. Devolve a lista e quantas entraram. */
internal fun addUnique(current: List<String>, new: List<String>): Pair<List<String>, Int> {
    val present = current.toMutableSet()
    val toAdd = new.filter { present.add(it) }
    return (current + toAdd) to toAdd.size
}

internal fun <T> moveItem(list: List<T>, from: Int, to: Int): List<T> {
    if (from !in list.indices || to !in list.indices || from == to) return list
    val mutable = list.toMutableList()
    mutable.add(to, mutable.removeAt(from))
    return mutable
}

private fun readPlaylists(file: File): List<Playlist> {
    if (!file.exists()) return emptyList()
    val array = JSONArray(file.readText())
    return (0 until array.length()).map { index ->
        val json = array.getJSONObject(index)
        val tracks = json.optJSONArray("tracks")
        Playlist(
            id = json.getLong("id"),
            name = json.optString("name"),
            trackUris = (0 until (tracks?.length() ?: 0)).map { tracks!!.getString(it) },
            createdAtMs = json.optLong("created"),
            updatedAtMs = json.optLong("updated"),
        )
    }
}

private fun writePlaylists(file: File, playlists: List<Playlist>) {
    val array = JSONArray()
    playlists.forEach { playlist ->
        array.put(
            JSONObject()
                .put("id", playlist.id)
                .put("name", playlist.name)
                .put("tracks", JSONArray(playlist.trackUris))
                .put("created", playlist.createdAtMs)
                .put("updated", playlist.updatedAtMs),
        )
    }
    val temp = File(file.parentFile, file.name + ".tmp")
    temp.writeText(array.toString())
    temp.renameTo(file)
}

/** Ponto de acesso provisório enquanto o app não tem Hilt. */
object PlaylistData {
    @Volatile
    private var instance: PlaylistRepository? = null

    fun repository(context: Context): PlaylistRepository =
        instance ?: synchronized(this) {
            instance ?: FilePlaylistRepository(context.applicationContext).also { instance = it }
        }
}
