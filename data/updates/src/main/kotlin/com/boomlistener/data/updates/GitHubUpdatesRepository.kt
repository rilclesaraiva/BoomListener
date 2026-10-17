// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.updates

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

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
        }
        try {
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    parseRelease(body, current)
                }
                // A API responde 404 quando ainda não existe nenhuma Release.
                HttpURLConnection.HTTP_NOT_FOUND -> UpdateCheckResult.NoReleases
                else -> UpdateCheckResult.Failed
            }
        } catch (_: Exception) {
            UpdateCheckResult.Failed
        } finally {
            connection.disconnect()
        }
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
        )
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
