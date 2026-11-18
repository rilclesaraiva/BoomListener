// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.profile

import com.boomlistener.data.wellbeing.ListeningCalendar
import com.boomlistener.data.wellbeing.StatsReport
import java.time.LocalDate
import kotlin.math.roundToInt

/** Um destaque do mês no cartão laranja do Perfil: o que mais marcou o mês. */
internal sealed interface Highlight {
    data class TopArtist(val name: String, val plays: Int) : Highlight
    data class TopSong(val uri: String, val title: String, val artist: String?, val plays: Int) : Highlight
    data class TopGenre(val genre: String, val percent: Int) : Highlight
    data class BestDay(val date: LocalDate, val minutes: Int) : Highlight
}

/** O que o Perfil mostra além do histórico: os destaques do mês e o calendário de escuta. */
internal data class ProfileInsights(val highlights: List<Highlight>, val calendar: ListeningCalendar)

/** Artista, música, gênero e dia recorde do mês (os que existirem), nessa ordem. */
internal fun highlightsOf(report: StatsReport): List<Highlight> = buildList {
    report.artists.firstOrNull()?.let { add(Highlight.TopArtist(it.name, it.plays)) }
    report.songs.firstOrNull()?.let { add(Highlight.TopSong(it.uri, it.title, it.artist, it.plays)) }
    report.genres.firstOrNull()?.let { add(Highlight.TopGenre(it.genre, (it.share * 100).roundToInt())) }
    report.bestDay?.takeIf { it.minutes > 0 }?.let { add(Highlight.BestDay(it.date, it.minutes)) }
}
