// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.profile

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.boomlistener.core.designsystem.component.BoomArtwork
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.designsystem.component.BoomImageCycle
import com.boomlistener.core.designsystem.component.rememberImageSet
import com.boomlistener.core.designsystem.theme.boomFastSpatialSpec
import com.boomlistener.core.designsystem.theme.boomSpatialSpec
import com.boomlistener.data.wellbeing.CalendarDay
import com.boomlistener.data.wellbeing.ListeningCalendar
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// Quanto tempo cada destaque do mês fica na tela antes de passar para o próximo.
private const val HIGHLIGHT_MS = 5_000L

/**
 * "Este mês": músicas, horas e artistas, com os destaques do mês trocando embaixo
 * (artista, música, gênero e dia recorde) e uma seta que mostra que tocar abre as Estatísticas.
 */
@Composable
internal fun MonthCard(
    state: MyProfileUiState,
    highlights: List<Highlight>,
    viewModel: MyProfileViewModel,
    onClick: () -> Unit,
) {
    val month = state.today.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
        .replaceFirstChar { it.titlecase(Locale.getDefault()) }
    val hours = state.stats.monthMinutes / 60
    val minutes = state.stats.monthMinutes % 60
    val onContainer = MaterialTheme.colorScheme.onPrimaryContainer
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = onContainer,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(BoomIcons.History, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(stringResource(R.string.feature_profile_this_month).uppercase(), style = MaterialTheme.typography.titleSmall)
                    Text(month, style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.weight(1f))
                // "Ver tudo ›": o sinal claro de que tocar abre as Estatísticas completas.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(onContainer.copy(alpha = 0.18f))
                        .padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                ) {
                    Text(stringResource(R.string.feature_profile_see_all), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Icon(BoomIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigNumber(state.stats.monthPlays.toString(), pluralStringResource(R.plurals.feature_profile_songs, state.stats.monthPlays), Modifier.weight(1f))
                BigNumber(
                    if (hours > 0) stringResource(R.string.feature_profile_hours_minutes, hours, minutes) else stringResource(R.string.feature_profile_minutes, minutes),
                    stringResource(R.string.feature_profile_listened),
                    Modifier.weight(1.4f),
                )
                BigNumber(state.stats.monthArtists.toString(), pluralStringResource(R.plurals.feature_profile_artists, state.stats.monthArtists), Modifier.weight(1f))
            }
            if (highlights.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(top = 16.dp, bottom = 14.dp), color = onContainer.copy(alpha = 0.22f))
                HighlightPager(highlights, viewModel)
            }
        }
    }
}

@Composable
private fun HighlightPager(highlights: List<Highlight>, viewModel: MyProfileViewModel) {
    val pager = rememberPagerState { highlights.size }
    if (highlights.size > 1) {
        val spec = boomSpatialSpec<Float>()
        // Passa sozinho para o próximo; deslizar à mão reinicia a contagem. A chave é a página
        // *assentada*: `currentPage` muda no meio da animação e cancelaria a própria troca.
        LaunchedEffect(highlights.size, pager.settledPage) {
            delay(HIGHLIGHT_MS)
            pager.animateScrollToPage((pager.settledPage + 1) % highlights.size, animationSpec = spec)
        }
    }
    HorizontalPager(pager, pageSpacing = 16.dp) { page -> HighlightRow(highlights[page], viewModel) }
    if (highlights.size > 1) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        ) {
            repeat(highlights.size) { index ->
                val selected = index == pager.currentPage
                val width by animateDpAsState(if (selected) 18.dp else 6.dp, boomFastSpatialSpec(), label = "dot")
                Box(
                    Modifier
                        .width(width)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(contentAlpha(if (selected) 1f else 0.4f)),
                )
            }
        }
    }
}

@Composable
private fun contentAlpha(alpha: Float) = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = alpha)

@Composable
private fun HighlightRow(highlight: Highlight, viewModel: MyProfileViewModel) {
    val lead = 44.dp
    val label: String
    val value: String
    val caption: String
    when (highlight) {
        is Highlight.TopArtist -> {
            label = stringResource(R.string.feature_profile_highlight_artist)
            value = highlight.name
            caption = pluralStringResource(R.plurals.feature_profile_plays, highlight.plays, highlight.plays)
        }
        is Highlight.TopSong -> {
            label = stringResource(R.string.feature_profile_highlight_song)
            value = highlight.title
            caption = pluralStringResource(R.plurals.feature_profile_plays, highlight.plays, highlight.plays)
        }
        is Highlight.TopGenre -> {
            label = stringResource(R.string.feature_profile_highlight_genre)
            value = highlight.genre
            caption = stringResource(R.string.feature_profile_highlight_genre_share, highlight.percent)
        }
        is Highlight.BestDay -> {
            label = stringResource(R.string.feature_profile_highlight_day)
            value = highlight.date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
            caption = stringResource(R.string.feature_profile_highlight_day_time, shortDuration(highlight.minutes))
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        when (highlight) {
            is Highlight.TopArtist -> {
                val px = with(LocalDensity.current) { lead.roundToPx() }
                val photos = rememberImageSet(remember(highlight.name, px) { artistAttempts(highlight.name, px, viewModel) })
                BoomImageCycle(photos) { shown -> Avatar(highlight.name, size = lead, image = shown) }
            }
            is Highlight.TopSong -> BoomArtwork(
                image = rememberLocalArtwork(viewModel, highlight.uri, lead),
                fallbackText = highlight.title,
                size = lead,
            )
            // Gênero não leva ícone: o texto começa na borda.
            is Highlight.TopGenre -> Unit
            is Highlight.BestDay -> Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(lead),
            ) {
                Box(contentAlignment = Alignment.Center) { Icon(BoomIcons.Timer, contentDescription = null) }
            }
        }
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(caption, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun BigNumber(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

/**
 * Calendário de escuta: um quadradinho por dia, mais forte quanto mais você ouviu. Tocar num dia
 * mostra a data e o tempo dele (tocar de novo ou fora dos quadradinhos limpa) e arrastar o dedo na horizontal passa de dia em dia. Não abre nada: quem abre
 * as Estatísticas é o cartão laranja.
 */
@Composable
internal fun CalendarCard(calendar: ListeningCalendar, selected: LocalDate?, onSelect: (LocalDate?) -> Unit) {
    val gap = 3.dp
    val currentSelected by rememberUpdatedState(selected)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val selectedDay = remember(calendar, selected) { calendar.weeks.flatten().firstOrNull { it != null && it.date == selected } }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.feature_profile_calendar_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        pluralStringResource(R.plurals.feature_profile_calendar_weeks, calendar.weeks.size, calendar.weeks.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(gap),
                // Chave só no calendário: marcar um dia (selected muda) não pode reiniciar o gesto,
                // senão o arrasto parava no primeiro quadrado.
                modifier = Modifier.pointerInput(calendar) {
                    val gapPx = gap.toPx()
                    // O quadrado sob o dedo (os quadrados são redondos de lado igual; fora da grade, o mais próximo).
                    fun dayAt(position: Offset): CalendarDay? {
                        val columns = calendar.weeks.size
                        val cell = (size.width - gapPx * (columns - 1)) / columns
                        val column = (position.x / (cell + gapPx)).toInt().coerceIn(0, columns - 1)
                        val row = (position.y / (cell + gapPx)).toInt().coerceIn(0, 6)
                        return calendar.weeks[column][row]
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val tapped = dayAt(down.position)
                        // Só o arrasto na horizontal é nosso; na vertical a página rola normalmente.
                        val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                        if (drag == null) {
                            // Toque sem arrastar: marca o dia, ou desmarca se já era o marcado.
                            val up = currentEvent.changes.firstOrNull { it.id == down.id }
                            if (tapped != null && up != null && !up.pressed && !up.isConsumed) {
                                currentOnSelect(if (currentSelected == tapped.date) null else tapped.date)
                                // Consumido: o toque fora dos quadradinhos (que limpa a seleção) não o vê.
                                up.consume()
                            }
                        } else {
                            dayAt(drag.position)?.let { currentOnSelect(it.date) }
                            horizontalDrag(drag.id) { change ->
                                change.consume()
                                dayAt(change.position)?.let { currentOnSelect(it.date) }
                            }
                        }
                    }
                },
            ) {
                calendar.weeks.forEach { week ->
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(gap)) {
                        week.forEach { day ->
                            // Dia que ainda não chegou fica vazio.
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .then(
                                        if (day == null) {
                                            Modifier
                                        } else {
                                            Modifier
                                                .clip(MaterialTheme.shapes.extraSmall)
                                                .background(calendarLevelColor(day.level))
                                                .then(if (day.date == selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, MaterialTheme.shapes.extraSmall) else Modifier)
                                        },
                                    ),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    selectedDay?.let { day ->
                        val date = day.date.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()))
                        // Dia com escuta menor que 1 minuto conta como 1 minuto, para não mostrar "0 min".
                        if (day.level == 0) stringResource(R.string.feature_profile_calendar_day_none, date)
                        else stringResource(R.string.feature_profile_calendar_day, date, shortDuration(day.minutes.coerceAtLeast(1)))
                    } ?: pluralStringResource(R.plurals.feature_profile_calendar_streak, calendar.longestStreak, calendar.longestStreak),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(stringResource(R.string.feature_profile_calendar_less), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                (0..4).forEach { level ->
                    Box(Modifier.padding(start = 4.dp).size(12.dp).clip(MaterialTheme.shapes.extraSmall).background(calendarLevelColor(level)))
                }
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.feature_profile_calendar_more), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun calendarLevelColor(level: Int) = when (level) {
    0 -> MaterialTheme.colorScheme.surfaceContainerHighest
    1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    3 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
    else -> MaterialTheme.colorScheme.primary
}

/**
 * O Wrapped do ano espera aqui: em dezembro, quando a notificação não saiu (sem permissão)
 * e a pessoa ainda não dispensou. Nas builds de teste (registro de diagnóstico ligado) ele
 * aparece sempre, para poder abrir o Wrapped a qualquer momento.
 */
@Composable
internal fun WrappedReadyCard(year: Int, onOpen: () -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onOpen,
        color = colors.tertiaryContainer,
        contentColor = colors.onTertiaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Box {
            // Enfeite: círculos no canto, no tom do cartão.
            Box(Modifier.align(Alignment.TopEnd).offset(x = 30.dp, y = (-30).dp).size(140.dp).clip(CircleShape).background(colors.tertiary.copy(alpha = 0.25f)))
            Box(Modifier.align(Alignment.TopEnd).offset(x = (-34).dp, y = 48.dp).size(64.dp).clip(CircleShape).background(colors.tertiary.copy(alpha = 0.5f)))
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 10.dp)) {
                Text(stringResource(R.string.feature_profile_wrapped_card_label, year), style = MaterialTheme.typography.labelLarge)
                Text(
                    stringResource(R.string.feature_profile_wrapped_card_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(end = 64.dp),
                )
                Text(stringResource(R.string.feature_profile_wrapped_card_sub), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = onOpen,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.tertiary, contentColor = colors.onTertiary),
                    ) {
                        Text(stringResource(R.string.feature_profile_wrapped_card_open))
                        Icon(BoomIcons.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_profile_wrapped_card_dismiss)) }
                }
            }
        }
    }
}
