// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.releases

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Um artista seguido: o radar de lançamentos acompanha este id do Deezer. */
data class FollowedArtist(val deezerId: Long, val name: String)

/**
 * Artistas que o usuário segue. Hoje só guarda a lista (no aparelho); o radar
 * diário de lançamentos que usa esta lista entra numa próxima versão.
 */
interface FollowRepository {
    val followed: Flow<List<FollowedArtist>>
    suspend fun follow(artist: FollowedArtist)
    suspend fun unfollow(deezerId: Long)
}

private val Context.followStore: DataStore<Preferences> by preferencesDataStore(name = "followed_artists")
private val FOLLOWED = stringSetPreferencesKey("followed")

internal class DataStoreFollowRepository(context: Context) : FollowRepository {
    private val store = context.followStore

    override val followed: Flow<List<FollowedArtist>> = store.data.map { prefs ->
        prefs[FOLLOWED].orEmpty().mapNotNull(::decodeFollowed).sortedBy { it.name.lowercase() }
    }

    override suspend fun follow(artist: FollowedArtist) {
        store.edit { prefs ->
            val current = prefs[FOLLOWED].orEmpty().filterNot { decodeFollowed(it)?.deezerId == artist.deezerId }
            prefs[FOLLOWED] = (current + encodeFollowed(artist)).toSet()
        }
    }

    override suspend fun unfollow(deezerId: Long) {
        store.edit { prefs ->
            prefs[FOLLOWED] = prefs[FOLLOWED].orEmpty().filterNot { decodeFollowed(it)?.deezerId == deezerId }.toSet()
        }
    }
}

/** "123|Nome do artista" (o nome pode ter "|"; só o primeiro separa). */
internal fun encodeFollowed(artist: FollowedArtist): String = "${artist.deezerId}|${artist.name}"

internal fun decodeFollowed(raw: String): FollowedArtist? {
    val separator = raw.indexOf('|')
    if (separator <= 0) return null
    val id = raw.substring(0, separator).toLongOrNull() ?: return null
    return FollowedArtist(id, raw.substring(separator + 1))
}

/** Ponto de acesso provisório enquanto o app não tem Hilt. */
object ReleasesData {
    @Volatile
    private var instance: FollowRepository? = null

    fun follows(context: Context): FollowRepository =
        instance ?: synchronized(this) {
            instance ?: DataStoreFollowRepository(context.applicationContext).also { instance = it }
        }

    @Volatile
    private var choices: ReleasesChoicesRepository? = null

    fun choices(context: Context): ReleasesChoicesRepository =
        choices ?: synchronized(this) {
            choices ?: PrefsReleasesChoices(context.applicationContext).also { choices = it }
        }
}
