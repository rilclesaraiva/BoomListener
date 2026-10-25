// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.player

import kotlin.math.pow

/*
 * ReplayGain: muitas músicas trazem nas tags quanto o volume delas precisa
 * subir ou descer para todas soarem parecidas (REPLAYGAIN_TRACK_GAIN, em dB;
 * no Opus, R128_TRACK_GAIN, em 1/256 dB e com referência 5 dB mais baixa).
 *
 * O volume do player vai de 0 a 1, então só dá para baixar: música que
 * pediria para subir fica no volume normal. Puro e testado.
 */

private val GAIN_NUMBER = Regex("""[-+]?\d+(?:[.,]\d+)?""")

/** O ganho em dB lido das tags [tags] (nome → valor, nome sem diferença de maiúsculas), ou null. */
internal fun replayGainDb(tags: List<Pair<String, String>>): Float? {
    fun find(name: String) = tags.firstOrNull { it.first.equals(name, ignoreCase = true) }?.second
    find("REPLAYGAIN_TRACK_GAIN")?.let { value ->
        GAIN_NUMBER.find(value)?.value?.replace(',', '.')?.toFloatOrNull()?.let { return it }
    }
    find("R128_TRACK_GAIN")?.trim()?.toIntOrNull()?.let { q78 ->
        // R128 mira -23 LUFS; o ReplayGain, -18. Daí os 5 dB.
        return q78 / 256f + R128_TO_REPLAYGAIN_DB
    }
    return null
}

/** Fator de volume (0–1) para o ganho [db]; sem ganho, volume normal. */
internal fun replayGainVolume(db: Float?): Float =
    if (db == null) 1f else 10f.pow(db / 20f).coerceIn(0f, 1f)

private const val R128_TO_REPLAYGAIN_DB = 5f
