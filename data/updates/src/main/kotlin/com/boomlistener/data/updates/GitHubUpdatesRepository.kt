// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.updates

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val KEY_SNOOZED_UNTIL = "snoozed_until"
private const val KEY_SKIPPED = "skipped_version"
private const val KEY_LAST_CHECK = "last_auto_check"
private const val KEY_CACHED_RELEASE = "cached_release"
private const val KEY_ETAG = "cached_release_etag"

private const val LATEST_RELEASE_URL =
    "https://api.github.com/repos/rilclesaraiva/BoomListener/releases/latest"

internal class GitHubUpdatesRepository(
    private val context: Context,
) : UpdatesRepository {

    override suspend fun checkForUpdates(): UpdateCheckResult = withContext(Dispatchers.IO) {
        val current = installedVersion()
        val connection = (URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "BoomListener/$current")
            // Pergunta "mudou desde a última?": a resposta 304 (nada mudou) não
            // gasta o limite de consultas do GitHub, então dá para conferir a
            // cada abertura do app.
            val etag = prefs.getString(KEY_ETAG, null)
            if (etag != null && prefs.contains(KEY_CACHED_RELEASE)) setRequestProperty("If-None-Match", etag)
        }
        val result = try {
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    // Guarda a Release mais recente para a próxima abertura.
                    prefs.edit()
                        .putString(KEY_CACHED_RELEASE, body)
                        .putString(KEY_ETAG, connection.getHeaderField("ETag"))
                        .apply()
                    parseRelease(body, current)
                }
                // Nada mudou desde a última consulta: vale a Release guardada.
                HttpURLConnection.HTTP_NOT_MODIFIED -> {
                    val body = prefs.getString(KEY_CACHED_RELEASE, null)
                    if (body == null) UpdateCheckResult.Failed else parseRelease(body, current)
                }
                // A API responde 404 quando ainda não existe nenhuma Release.
                HttpURLConnection.HTTP_NOT_FOUND -> {
                    prefs.edit().remove(KEY_CACHED_RELEASE).remove(KEY_ETAG).apply()
                    UpdateCheckResult.NoReleases
                }
                else -> UpdateCheckResult.Failed
            }
        } catch (_: Exception) {
            UpdateCheckResult.Failed
        } finally {
            connection.disconnect()
        }
        result
    }

    override fun cachedUpdate(): UpdateCheckResult.Available? {
        val body = prefs.getString(KEY_CACHED_RELEASE, null) ?: return null
        return runCatching { parseRelease(body, installedVersion()) as? UpdateCheckResult.Available }.getOrNull()
    }

    private fun parseRelease(body: String, current: String): UpdateCheckResult {
        val json = JSONObject(body)
        val latest = json.getString("tag_name").removePrefix("v")
        if (!isNewerVersion(latest, current)) return UpdateCheckResult.UpToDate(current)

        val assets = json.optJSONArray("assets")
        val apkUrl = (0 until (assets?.length() ?: 0))
            .map { assets!!.getJSONObject(it) }
            .firstOrNull { it.getString("name").endsWith(".apk") }
            ?.getString("browser_download_url")
        return UpdateCheckResult.Available(
            currentVersion = current,
            latestVersion = latest,
            pageUrl = json.getString("html_url"),
            downloadUrl = apkUrl,
            notes = json.optString("body", "").takeIf { it != "null" }.orEmpty(),
        )
    }

    private val prefs = context.getSharedPreferences("update_prompt", Context.MODE_PRIVATE)

    override fun promptState() = UpdatePromptState(
        snoozedUntilMs = prefs.getLong(KEY_SNOOZED_UNTIL, 0),
        skippedVersion = prefs.getString(KEY_SKIPPED, null),
        lastAutoCheckMs = prefs.getLong(KEY_LAST_CHECK, 0),
    )

    override fun snoozePrompt(untilMs: Long) {
        prefs.edit().putLong(KEY_SNOOZED_UNTIL, untilMs).apply()
    }

    override fun skipVersion(version: String) {
        prefs.edit().putString(KEY_SKIPPED, version).apply()
    }

    override fun resumePrompts() {
        prefs.edit().remove(KEY_SNOOZED_UNTIL).remove(KEY_SKIPPED).apply()
    }

    override fun markAutoCheck(atMs: Long) {
        prefs.edit().putLong(KEY_LAST_CHECK, atMs).apply()
    }

    @Suppress("DEPRECATION")
    private fun installedVersion(): String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull()
            ?.removeSuffix("-debug")
            ?: "0.0.0"
}

/**
 * true se [candidate] é uma versão mais nova que [installed], comparando
 * MAJOR.MINOR.PATCH como números ("0.10.0" é mais nova que "0.9.0").
 * Partes que não são número contam como 0.
 */
internal fun isNewerVersion(candidate: String, installed: String): Boolean {
    fun parts(version: String) = version.trim().removePrefix("v").split(".", "-")
        .take(3).map { it.toIntOrNull() ?: 0 }.let { it + List(3 - it.size) { 0 } }
    val a = parts(candidate)
    val b = parts(installed)
    for (i in 0 until 3) {
        if (a[i] != b[i]) return a[i] > b[i]
    }
    return false
}
