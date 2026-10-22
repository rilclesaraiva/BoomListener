// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.app

import android.app.Application
import com.boomlistener.core.player.PlaybackHooks
import com.boomlistener.data.wellbeing.HistoryData
import com.boomlistener.data.wellbeing.HistoryEntry
import com.boomlistener.data.wellbeing.StatsData
import com.boomlistener.data.profile.ProfileData
import com.boomlistener.feature.releases.ReleaseAlerts
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
        val history = HistoryData.repository(this)
        // Abre o perfil já no início: na primeira vez depois da versão com
        // amigos, traz nome, foto, capa e cor e apaga o que era do social.
        ProfileData.repository(this)
        // Confere lançamentos dos artistas seguidos a cada ~12 h (avisos no celular).
        ReleaseAlerts.schedule(this)
        PlaybackHooks.onListened = { play ->
            appScope.launch {
                history.record(HistoryEntry(play.uri, play.title, play.artist, play.album, play.timestampMs))
            }
        }
        PlaybackHooks.onPlayCounted = { play ->
            appScope.launch {
                stats.recordPlay(play.uri, play.title, play.artist, play.album, play.timestampMs)
            }
        }
    }
}
