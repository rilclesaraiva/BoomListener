// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.model

/**
 * Uma playlist do próprio app. Guarda os endereços das músicas ([trackUris],
 * na ordem escolhida); a música em si continua no MediaStore.
 */
data class Playlist(
    val id: Long,
    val name: String,
    val trackUris: List<String>,
    val createdAtMs: Long,
    val updatedAtMs: Long,
)
