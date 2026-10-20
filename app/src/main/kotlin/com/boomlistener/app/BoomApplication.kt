// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.app

import android.app.Application
import com.boomlistener.core.player.PlaybackHooks
import com.boomlistener.data.wellbeing.HistoryData
import com.boomlistener.data.wellbeing.HistoryEntry
import com.boomlistener.data.wellbeing.StatsData
import com.boomlistener.data.social.SocialData
import com.boomlistener.data.social.TrackInfo
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
        val social = SocialData.repository(this)
        PlaybackHooks.onListened = { play ->
            appScope.launch {
                history.record(HistoryEntry(play.uri, play.title, play.artist, play.album, play.timestampMs))
                // Amigos: só vai se a pessoa ligou "compartilhar o que estou ouvindo".
                social.publishListened(TrackInfo(play.title, play.artist, play.album))
            }
        }
        // "Ouvindo agora": manda só quando muda (tocar, trocar de música, pausar).
        var lastNow: TrackInfo? = null
        var sentOnce = false
        PlaybackHooks.onNowPlaying = { play ->
            val track = play?.let { TrackInfo(it.title, it.artist, it.album) }
            if (!sentOnce || track != lastNow) {
                sentOnce = true
                lastNow = track
                appScope.launch { social.publishNowPlaying(track) }
            }
        }
        // Amigos: mensagem nova com o app fora da tela vira notificação.
        val notifier = MessageNotifier(this)
        appScope.launch { social.incoming.collect { message -> notifier.show(message) } }
        PlaybackHooks.onPlayCounted = { play ->
            appScope.launch {
                stats.recordPlay(play.uri, play.title, play.artist, play.album, play.timestampMs)
            }
        }
    }
}
