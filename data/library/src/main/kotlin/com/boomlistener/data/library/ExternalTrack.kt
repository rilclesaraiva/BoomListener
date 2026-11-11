// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.library

/** Título de uma música sem tag: o nome do arquivo sem a extensão ("A way(M4A_128K).m4a" → "A way(M4A_128K)"). */
internal fun titleFromFileName(fileName: String?): String? {
    val name = fileName?.trim().orEmpty()
    val dot = name.lastIndexOf('.')
    val base = if (dot > 0 && name.length - dot - 1 in 1..5) name.substring(0, dot) else name
    return base.takeIf { it.isNotBlank() }
}

/** O primeiro número de uma tag como "3/12" (faixa 3 de 12); null se não houver. */
internal fun leadingNumber(raw: String?): Int? =
    raw?.trim()?.takeWhile { it.isDigit() }?.toIntOrNull()?.takeIf { it > 0 }
