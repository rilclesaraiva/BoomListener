// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.app

import com.boomlistener.core.common.OfflineMode
import android.app.Application
import com.boomlistener.core.player.PlaybackHooks
import com.boomlistener.data.library.LibraryData
import com.boomlistener.data.wellbeing.DailyStatsData
import com.boomlistener.data.wellbeing.HistoryData
import com.boomlistener.data.wellbeing.HistoryEntry
import com.boomlistener.data.wellbeing.StatsData
import com.boomlistener.data.wellbeing.splitGenres
import com.boomlistener.data.profile.ProfileData
import com.boomlistener.feature.profile.WrappedNotification
import com.boomlistener.feature.releases.ReleaseAlerts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
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
        // Antes de tudo: com o modo offline ligado, nada pode ir para a internet.
        OfflineMode.init(this)
        val stats = StatsData.repository(this)
        val history = HistoryData.repository(this)
        val daily = DailyStatsData.repository(this)
        // Abre o perfil já no início: na primeira vez depois da versão com
        // amigos, traz nome, foto, capa e cor e apaga o que era do social.
        ProfileData.repository(this)
        // Confere lançamentos dos artistas seguidos a cada ~12 h (avisos no celular).
        ReleaseAlerts.schedule(this)
        WrappedNotification.schedule(this)
        PlaybackHooks.onListened = { play ->
            appScope.launch {
                history.record(HistoryEntry(play.uri, play.title, play.artist, play.album, play.timestampMs))
                daily.recordListen(play.uri, play.title, play.artist, play.album, splitGenres(play.genre), play.timestampMs)
            }
        }
        // Resumo diário: o tempo realmente ouvido (músicas pulada ou ouvida até o fim).
        PlaybackHooks.onListenedTime = { listened ->
            appScope.launch {
                val play = listened.play
                daily.recordTime(play.uri, play.title, play.artist, play.album, splitGenres(play.genre), listened.ms, listened.timestampMs)
            }
        }
        // Traz o histórico antigo para o resumo diário (uma vez). Sem a biblioteca disponível
        // (permissão ainda não dada), espera a próxima abertura para saber os gêneros.
        appScope.launch {
            runCatching {
                if (daily.isMigrated()) return@runCatching
                val tracks = LibraryData.repository(this@BoomApplication).getTracks()
                if (tracks.isEmpty() && history.history.first().isNotEmpty()) return@runCatching
                val genres = tracks.associate { it.uri to splitGenres(it.genre) }
                daily.migrateIfNeeded { genres[it].orEmpty() }
            }
        }
        PlaybackHooks.onPlayCounted = { play ->
            appScope.launch {
                stats.recordPlay(play.uri, play.title, play.artist, play.album, play.timestampMs)
            }
        }
    }
}
