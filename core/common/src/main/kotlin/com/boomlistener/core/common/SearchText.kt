// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.common

import java.text.Normalizer

private val DIACRITICS = Regex("\\p{Mn}+")

/**
 * Versão do texto própria para busca: minúsculo e sem acentos, para que
 * "joao" encontre "João" e "ACAI" encontre "açaí".
 */
fun String.normalizedForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(DIACRITICS, "")
        .lowercase()
        .trim()
