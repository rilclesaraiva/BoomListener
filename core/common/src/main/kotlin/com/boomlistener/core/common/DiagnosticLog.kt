// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.common

import android.content.Context
import java.io.File
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Registro de diagnóstico (Ajustes → Sistema; desligado por padrão): guarda no
 * aparelho, em texto, o que o app faz num momento difícil de reproduzir (hoje, o teclado
 * e a confirmação de apagar), para a pessoa mandar ao mantenedor. Nada é enviado
 * sozinho; só entram nomes de telas, contagens e estados, nunca título, artista ou
 * endereço de música.
 */
object DiagnosticLog {
    private const val PREFS = "diagnostic_log"
    private const val KEY = "enabled"
    private const val FILE = "diagnostic.log"
    internal const val MAX_CHARS = 40_000

    private val state = MutableStateFlow(false)
    private val time = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    @Volatile
    private var file: File? = null

    /** Lê a escolha guardada (chamado ao abrir o app). */
    fun init(context: Context) {
        if (file != null) return
        file = File(context.noBackupFilesDir, FILE)
        state.value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false)
    }

    val enabled: StateFlow<Boolean>
        get() = state.asStateFlow()

    fun set(context: Context, on: Boolean) {
        init(context)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, on).apply()
        state.value = on
        if (on) log("registro", "ligado")
    }

    /** Anota uma linha ("hora tipo: mensagem"); sem efeito com o registro desligado. */
    fun log(tag: String, message: String) {
        if (!state.value) return
        val target = file ?: return
        synchronized(this) {
            runCatching {
                val line = formatLogLine(LocalTime.now().format(time), tag, message)
                target.appendText(line)
                if (target.length() > MAX_CHARS) target.writeText(trimLog(target.readText(), MAX_CHARS / 2))
            }
        }
    }

    /** O registro todo, para compartilhar. */
    fun read(): String = synchronized(this) { runCatching { file?.takeIf { it.exists() }?.readText() }.getOrNull().orEmpty() }

    fun clear() {
        synchronized(this) { runCatching { file?.delete() } }
    }
}

internal fun formatLogLine(time: String, tag: String, message: String): String = "$time $tag: $message\n"

/** Fica com o fim do texto (o mais recente), começando numa linha inteira. */
internal fun trimLog(text: String, maxChars: Int): String {
    if (text.length <= maxChars) return text
    val tail = text.takeLast(maxChars)
    val firstBreak = tail.indexOf('\n')
    return if (firstBreak >= 0) tail.substring(firstBreak + 1) else tail
}
