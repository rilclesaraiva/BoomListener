// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.player

/** Modo de repetição, na ordem em que o botão alterna. */
enum class RepeatMode { Off, All, One }

/** A música que está tocando, do jeito que as telas precisam mostrar. */
data class NowPlayingItem(
    val uri: String,
    val title: String,
    val artist: String?,
    val album: String?,
)

/**
 * Uma música da fila. [index] é a posição dela na lista original (é o que se
 * usa para pular até ela); a ordem da lista de fila já é a ordem em que vão
 * tocar, inclusive no modo aleatório.
 */
data class QueueEntry(
    val index: Int,
    val item: NowPlayingItem,
)

/** Tudo o que as telas precisam saber do player, num retrato só. */
data class PlaybackState(
    val item: NowPlayingItem? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val shuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.Off,
    val queueIndex: Int = 0,
    val queueSize: Int = 0,
    /** A fila na ordem em que vai tocar. */
    val queue: List<QueueEntry> = emptyList(),
) {
    /** Progresso de 0 a 1, para barras de progresso. */
    val progress: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

/** Desligado → repetir tudo → repetir uma → desligado. */
fun RepeatMode.next(): RepeatMode = when (this) {
    RepeatMode.Off -> RepeatMode.All
    RepeatMode.All -> RepeatMode.One
    RepeatMode.One -> RepeatMode.Off
}
