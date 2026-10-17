// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.model

/**
 * Uma música da biblioteca do aparelho.
 *
 * [uri] é o endereço "content://" do arquivo no MediaStore, guardado como texto
 * para este módulo não depender do Android.
 */
data class Track(
    val id: Long,
    val uri: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val albumId: Long,
    val durationMs: Long,
    val trackNumber: Int?,
    val year: Int?,
    val dateAddedSeconds: Long,
)
