// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.releases

import android.content.Context
import com.boomlistener.core.model.Release
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Um lançamento novo de um artista seguido, para avisar. */
data class NewRelease(val artistId: Long, val release: Release)

/** Resultado de uma conferência: o que já é conhecido agora e o que avisar. */
data class ReleaseCheck(val known: Map<Long, Set<Long>>, val fresh: List<NewRelease>)

/** Só avisa lançamentos de até tantos dias atrás (um álbum antigo que apareceu agora não é novidade). */
const val ALERT_MAX_AGE_DAYS = 30L

/**
 * Compara a discografia de agora ([fetched], por artista) com a que já era
 * conhecida e devolve o que é novo. Regras:
 * - Artista visto pela primeira vez (acabou de ser seguido): tudo vira
 *   conhecido, sem aviso (senão seguir alguém soltaria dezenas de avisos).
 * - Só avisa lançamento com data de até [ALERT_MAX_AGE_DAYS] dias (e não futuro).
 * - Artista que falhou nesta conferência mantém o que era conhecido.
 * - Artista que deixou de ser seguido sai da lista.
 */
fun findNewReleases(
    followedIds: Set<Long>,
    known: Map<Long, Set<Long>>,
    fetched: Map<Long, List<Release>>,
    today: LocalDate,
): ReleaseCheck {
    val fresh = mutableListOf<NewRelease>()
    val next = mutableMapOf<Long, Set<Long>>()
    for (artistId in followedIds) {
        val releases = fetched[artistId]
        val before = known[artistId]
        when {
            releases == null -> before?.let { next[artistId] = it }
            before == null -> next[artistId] = releases.map { it.id }.toSet()
            else -> {
                releases.filter { it.id !in before && isRecent(it, today) }
                    .forEach { fresh += NewRelease(artistId, it) }
                next[artistId] = before + releases.map { it.id }
            }
        }
    }
    return ReleaseCheck(next, fresh.sortedByDescending { it.release.releaseDate.orEmpty() })
}

private fun isRecent(release: Release, today: LocalDate): Boolean {
    val date = release.releaseDate?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() } ?: return false
    return !date.isAfter(today) && !date.isBefore(today.minusDays(ALERT_MAX_AGE_DAYS))
}

/** Avisos de lançamentos: se estão ligados e o que já foi visto de cada artista. */
interface ReleaseAlertsRepository {
    val enabled: StateFlow<Boolean>
    fun setEnabled(enabled: Boolean)
    fun known(): Map<Long, Set<Long>>
    fun saveKnown(known: Map<Long, Set<Long>>)
    /** Já pediu a permissão de notificação uma vez (não pede de novo sozinho). */
    var askedPermission: Boolean
}

internal fun encodeKnown(known: Map<Long, Set<Long>>): Set<String> =
    known.flatMap { (artist, releases) -> if (releases.isEmpty()) listOf("$artist:") else releases.map { "$artist:$it" } }.toSet()

internal fun decodeKnown(raw: Set<String>): Map<Long, Set<Long>> {
    val result = mutableMapOf<Long, MutableSet<Long>>()
    for (item in raw) {
        val separator = item.indexOf(':')
        if (separator <= 0) continue
        val artist = item.substring(0, separator).toLongOrNull() ?: continue
        val set = result.getOrPut(artist) { mutableSetOf() }
        item.substring(separator + 1).toLongOrNull()?.let(set::add)
    }
    return result
}

internal class PrefsReleaseAlerts(context: Context) : ReleaseAlertsRepository {
    private val prefs = context.getSharedPreferences("release_alerts", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, true))
    override val enabled: StateFlow<Boolean> = state.asStateFlow()

    override fun setEnabled(enabled: Boolean) {
        state.value = enabled
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    override fun known(): Map<Long, Set<Long>> = decodeKnown(prefs.getStringSet(KEY_KNOWN, null).orEmpty())

    override fun saveKnown(known: Map<Long, Set<Long>>) {
        prefs.edit().putStringSet(KEY_KNOWN, encodeKnown(known)).apply()
    }

    override var askedPermission: Boolean
        get() = prefs.getBoolean(KEY_ASKED, false)
        set(value) = prefs.edit().putBoolean(KEY_ASKED, value).apply()

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val KEY_KNOWN = "known"
        const val KEY_ASKED = "asked_permission"
    }
}
