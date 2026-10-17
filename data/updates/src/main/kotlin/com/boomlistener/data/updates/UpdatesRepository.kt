// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.updates

import android.content.Context

/** Resultado de "Verificar atualizações". */
sealed interface UpdateCheckResult {
    /** A versão instalada já é a mais nova publicada. */
    data class UpToDate(val currentVersion: String) : UpdateCheckResult

    /** Existe versão mais nova. [downloadUrl] é o APK; [pageUrl] é a página da Release. */
    data class Available(
        val currentVersion: String,
        val latestVersion: String,
        val pageUrl: String,
        val downloadUrl: String?,
    ) : UpdateCheckResult

    /** O repositório ainda não tem nenhuma Release publicada. */
    data object NoReleases : UpdateCheckResult

    /** Sem internet, GitHub fora do ar, limite de consultas etc. */
    data object Failed : UpdateCheckResult
}

/** Confere se há uma versão mais nova na última Release do GitHub. */
interface UpdatesRepository {
    suspend fun checkForUpdates(): UpdateCheckResult
}

/** Ponto de acesso provisório enquanto o app não tem Hilt. */
object UpdatesData {
    @Volatile
    private var instance: UpdatesRepository? = null

    fun repository(context: Context): UpdatesRepository =
        instance ?: synchronized(this) {
            instance ?: GitHubUpdatesRepository(context.applicationContext).also { instance = it }
        }
}
