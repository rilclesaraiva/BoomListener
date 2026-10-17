// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.app

import android.app.Application
import com.boomlistener.core.player.PlaybackHooks
import com.boomlistener.data.wellbeing.StatsData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Liga as peças que não podem se conhecer direto (regra de dependências):
 * o player avisa quando uma música conta como ouvida, e as estatísticas guardam.
 * Roda antes de qualquer tela ou do serviço de reprodução.
 */
class BoomApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        val stats = StatsData.repository(this)
        PlaybackHooks.onPlayCounted = { play ->
            appScope.launch {
                stats.recordPlay(play.uri, play.title, play.artist, play.album, play.timestampMs)
            }
        }
    }
}
