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
        /** O que mudou (texto da Release, em Markdown). */
        val notes: String = "",
    ) : UpdateCheckResult

    /** O repositório ainda não tem nenhuma Release publicada. */
    data object NoReleases : UpdateCheckResult

    /** Sem internet, GitHub fora do ar, limite de consultas etc. */
    data object Failed : UpdateCheckResult
}

/**
 * Até quando o aviso de versão nova fica quieto: [snoozedUntilMs] (o usuário
 * pediu para adiar), [skippedVersion] (pediu para pular aquela versão) e
 * [lastAutoCheckMs] (última consulta automática, para não consultar o GitHub
 * a cada abertura do app).
 */
data class UpdatePromptState(
    val snoozedUntilMs: Long = 0,
    val skippedVersion: String? = null,
    val lastAutoCheckMs: Long = 0,
)

/** Confere se há uma versão mais nova na última Release do GitHub. */
interface UpdatesRepository {
    suspend fun checkForUpdates(): UpdateCheckResult

    fun promptState(): UpdatePromptState = UpdatePromptState()

    /** Não mostrar o aviso até [untilMs]. */
    fun snoozePrompt(untilMs: Long) {}

    /** Não avisar mais desta versão (só da próxima). */
    fun skipVersion(version: String) {}

    fun markAutoCheck(atMs: Long) {}
}

/** Intervalo mínimo entre consultas automáticas (ao abrir o app). */
const val AUTO_CHECK_INTERVAL_MS: Long = 6 * 60 * 60 * 1000L

/** Abrir o app agora deve consultar o GitHub? */
fun shouldAutoCheck(nowMs: Long, state: UpdatePromptState): Boolean =
    nowMs >= state.snoozedUntilMs && nowMs - state.lastAutoCheckMs >= AUTO_CHECK_INTERVAL_MS

/** O aviso da versão [latestVersion] pode aparecer sozinho agora? */
fun shouldPrompt(latestVersion: String, nowMs: Long, state: UpdatePromptState): Boolean =
    nowMs >= state.snoozedUntilMs && state.skippedVersion?.removePrefix("v") != latestVersion.removePrefix("v")

/** Ponto de acesso provisório enquanto o app não tem Hilt. */
object UpdatesData {
    @Volatile
    private var instance: UpdatesRepository? = null

    fun repository(context: Context): UpdatesRepository =
        instance ?: synchronized(this) {
            instance ?: GitHubUpdatesRepository(context.applicationContext).also { instance = it }
        }
}
