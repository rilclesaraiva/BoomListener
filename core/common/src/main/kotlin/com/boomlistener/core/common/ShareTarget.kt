// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.common

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Um app que recebe links compartilhados. */
data class ShareApp(val packageName: String, val label: String)

/**
 * App favorito para compartilhar (Ajustes → Compartilhar): os menus das
 * músicas ganham "Compartilhar com <app>", que vai direto para ele.
 */
object ShareTarget {
    private const val PREFS = "share_target"
    private const val KEY = "favorite_package"

    private val favorite = MutableStateFlow<ShareApp?>(null)
    private var loaded = false

    @Volatile
    private var next: String? = null

    fun favorite(context: Context): StateFlow<ShareApp?> {
        if (!loaded) {
            loaded = true
            val pkg = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            favorite.value = pkg?.let { resolve(context, it) }
        }
        return favorite.asStateFlow()
    }

    /** Escolhe o favorito (null tira). Devolve false se o app não recebe links. */
    fun setFavorite(context: Context, packageName: String?): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (packageName.isNullOrBlank()) {
            prefs.edit().remove(KEY).apply()
            favorite.value = null
            return true
        }
        val app = resolve(context, packageName.trim()) ?: return false
        prefs.edit().putString(KEY, app.packageName).apply()
        favorite.value = app
        return true
    }

    /** Os apps do aparelho que recebem texto compartilhado, em ordem alfabética. */
    fun candidates(context: Context): List<ShareApp> {
        val pm = context.packageManager
        return pm.queryIntentActivities(sendIntent(), PackageManager.MATCH_DEFAULT_ONLY)
            .map { info -> ShareApp(info.activityInfo.packageName, info.loadLabel(pm).toString()) }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    /** O próximo compartilhamento vai direto para [packageName]. */
    fun shareNextWith(packageName: String) {
        next = packageName
    }

    internal fun consumeNext(): String? = next.also { next = null }

    private fun resolve(context: Context, packageName: String): ShareApp? {
        val pm = context.packageManager
        val handles = pm.queryIntentActivities(sendIntent().setPackage(packageName), PackageManager.MATCH_DEFAULT_ONLY)
        val info = handles.firstOrNull() ?: return null
        return ShareApp(packageName, info.loadLabel(pm).toString())
    }

    private fun sendIntent() = Intent(Intent.ACTION_SEND).setType("text/plain")
}
