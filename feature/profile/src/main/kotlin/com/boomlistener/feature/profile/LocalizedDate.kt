// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.profile

import android.text.format.DateFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formato de data na ordem e com as palavras do idioma do aparelho: [skeleton] só diz quais partes
 * mostrar ("dMMM" = dia e mês abreviado; "EEEdMMM" = com o dia da semana). Assim "19 nov" em
 * português e "Nov 19" em inglês dos EUA, em vez de uma ordem fixa para todos.
 */
internal fun localizedDateFormatter(skeleton: String): DateTimeFormatter {
    val locale = Locale.getDefault()
    return DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
}
