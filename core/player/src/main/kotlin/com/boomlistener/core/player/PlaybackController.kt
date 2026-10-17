// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.player

import android.content.Context
import com.boomlistener.core.model.Track
import kotlinx.coroutines.flow.StateFlow

/**
 * Controle do player para as telas. A reprodução de verdade acontece no
 * [PlaybackService], que continua tocando com o app fechado; isto aqui é só o
 * "controle remoto" dele.
 */
interface PlaybackController {
    val state: StateFlow<PlaybackState>

    /** Substitui a fila por [tracks] e começa a tocar a de posição [startIndex]. */
    fun play(tracks: List<Track>, startIndex: Int)

    fun togglePlayPause()
    fun next()
    fun previous()
    fun seekTo(positionMs: Long)
    fun toggleShuffle()
    fun cycleRepeatMode()

    /** Para de tocar e esvazia a fila (some o mini player e a notificação). */
    fun stop()
}

/**
 * Ponto de acesso provisório enquanto o app não tem Hilt: sempre a mesma
 * instância, conectada ao serviço de reprodução.
 */
object PlayerConnection {
    @Volatile
    private var instance: PlaybackController? = null

    fun controller(context: Context): PlaybackController =
        instance ?: synchronized(this) {
            instance ?: Media3PlaybackController(context.applicationContext).also { instance = it }
        }
}
