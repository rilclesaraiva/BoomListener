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
import com.boomlistener.feature.social.NowPlayingCovers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
                if (social.account.value?.sharing == true) {
                    val cover = NowPlayingCovers.find(this@BoomApplication, play.title, play.artist, play.album)
                    social.publishListened(TrackInfo(play.title, play.artist, play.album, cover))
                }
            }
        }
        // "Ouvindo agora": manda só quando muda (tocar, trocar de música, pausar).
        // Vai primeiro sem capa (rápido) e de novo com a capa do Deezer, quando
        // achar: os amigos não têm o arquivo, então precisam de uma capa de fora.
        var lastNow: TrackInfo? = null
        var sentOnce = false
        var coverJob: Job? = null
        PlaybackHooks.onNowPlaying = { play ->
            val track = play?.let { TrackInfo(it.title, it.artist, it.album) }
            if (!sentOnce || track != lastNow?.copy(coverUrl = null)) {
                sentOnce = true
                lastNow = track
                coverJob?.cancel()
                coverJob = appScope.launch {
                    social.publishNowPlaying(track)
                    if (track != null && social.account.value?.sharing == true) {
                        val cover = NowPlayingCovers.find(this@BoomApplication, track.title, track.artist, track.album)
                        if (cover != null && lastNow == track) {
                            val withCover = track.copy(coverUrl = cover)
                            lastNow = withCover
                            social.publishNowPlaying(withCover)
                        }
                    }
                }
            }
        }
        PlaybackHooks.onPlayCounted = { play ->
            appScope.launch {
                stats.recordPlay(play.uri, play.title, play.artist, play.album, play.timestampMs)
            }
        }
    }
}
