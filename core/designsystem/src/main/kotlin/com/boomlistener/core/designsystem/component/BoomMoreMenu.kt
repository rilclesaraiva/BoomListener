// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.getValue
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import com.boomlistener.core.designsystem.R

/**
 * O app favorito para compartilhar (Ajustes → Compartilhar). O :app fornece;
 * [prepare] faz o próximo compartilhamento ir direto para ele.
 */
class FavoriteShare(val label: String, val prepare: () -> Unit)

val LocalFavoriteShare = staticCompositionLocalOf<FavoriteShare?> { null }

/**
 * O tipo de cada opção do menu: dá o ícone e a posição na lista. A ordem é
 * fixa, das ações mais usadas para as mais raras (como no Spotify), e o
 * "apagar" fica sempre por último, longe do dedo.
 */
enum class BoomMenuKind {
    Share,
    ShareWithFavorite,
    ShareCard,
    Lyrics,
    Visualizer,
    PlayNow,
    PlayNext,
    Mix,
    Favorite,
    AddToPlaylist,
    RemoveFromPlaylist,
    Queue,
    GoToAlbum,
    GoToArtist,
    SleepTimer,
    Speed,
    EditTags,
    Rename,
    Details,
    YoutubeMusic,
    Deezer,
    Other,
    Delete,
}

/** O ícone de cada tipo (fora do enum, para os testes não carregarem ícones). */
internal val BoomMenuKind.icon: ImageVector?
    get() = when (this) {
        BoomMenuKind.Share -> BoomIcons.Share
        BoomMenuKind.ShareWithFavorite -> BoomIcons.Send
        BoomMenuKind.ShareCard -> BoomIcons.Image
        BoomMenuKind.Lyrics -> BoomIcons.Article
        BoomMenuKind.Visualizer -> BoomIcons.GraphicEq
        BoomMenuKind.PlayNow -> BoomIcons.Play
        BoomMenuKind.PlayNext -> BoomIcons.QueuePlayNext
        BoomMenuKind.Mix -> BoomIcons.Shuffle
        BoomMenuKind.Favorite -> BoomIcons.FavoriteBorder
        BoomMenuKind.AddToPlaylist -> BoomIcons.PlaylistAdd
        BoomMenuKind.RemoveFromPlaylist -> BoomIcons.RemoveCircle
        BoomMenuKind.Queue -> BoomIcons.QueueMusic
        BoomMenuKind.GoToAlbum -> BoomIcons.Album
        BoomMenuKind.GoToArtist -> BoomIcons.Person
        BoomMenuKind.SleepTimer -> BoomIcons.Timer
        BoomMenuKind.Speed -> BoomIcons.Speed
        BoomMenuKind.EditTags -> BoomIcons.Edit
        BoomMenuKind.Rename -> BoomIcons.Edit
        BoomMenuKind.Details -> BoomIcons.Info
        BoomMenuKind.YoutubeMusic -> BoomIcons.OpenExternal
        BoomMenuKind.Deezer -> BoomIcons.OpenExternal
        BoomMenuKind.Other -> null
        BoomMenuKind.Delete -> BoomIcons.Delete
    }

/** Uma opção do menu de três pontinhos. */
data class BoomMenuAction(
    val label: String,
    val enabled: Boolean = true,
    val kind: BoomMenuKind = BoomMenuKind.Other,
    /** Ícone diferente do padrão do [kind] (ex.: coração cheio de quem já é favorita). */
    val icon: ImageVector? = null,
    val onClick: () -> Unit,
)

/** O topo do menu: a capa e o nome do que foi tocado (música, álbum, playlist…). */
class BoomMenuHeader(
    val title: String,
    val subtitle: String? = null,
    val image: ImageBitmap? = null,
    /** Foto redonda (artista). */
    val round: Boolean = false,
)

/**
 * Botão de três pontinhos com as opções de sempre para músicas e álbuns:
 * as [actions] próprias da tela e, por fim, compartilhar o link, ouvir no
 * YouTube Music e abrir no Deezer (cada uma só aparece se for passada).
 * Abre o [BoomMenuSheet].
 */
@Composable
fun BoomMoreMenu(
    actions: List<BoomMenuAction> = emptyList(),
    onShare: (() -> Unit)? = null,
    onYoutubeMusic: (() -> Unit)? = null,
    onDeezer: (() -> Unit)? = null,
    header: BoomMenuHeader? = null,
    // Quem quiser abrir o menu de outro jeito (segurar a linha) passa o estado aqui.
    openState: MutableState<Boolean> = remember { mutableStateOf(false) },
) {
    IconButton(onClick = { openState.value = true }) {
        Icon(BoomIcons.More, contentDescription = stringResource(R.string.core_designsystem_more))
    }
    BoomMenuSheet(openState, actions, onShare, onYoutubeMusic, onDeezer, header)
}

/**
 * O menu em si: um painel que sobe de baixo, com a capa e o nome do item no
 * topo e as opções com ícone embaixo, só as que fazem sentido para aquele
 * item (cada tela passa as suas). Itens sem botão ⋮ (abrem ao segurar) usam
 * este direto.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoomMenuSheet(
    openState: MutableState<Boolean>,
    actions: List<BoomMenuAction>,
    onShare: (() -> Unit)? = null,
    onYoutubeMusic: (() -> Unit)? = null,
    onDeezer: (() -> Unit)? = null,
    header: BoomMenuHeader? = null,
) {
    var open by openState
    if (!open) return
    val favorite = LocalFavoriteShare.current
    val all = buildList {
        addAll(actions)
        onShare?.let { add(BoomMenuAction(stringResource(R.string.core_designsystem_share), kind = BoomMenuKind.Share, onClick = it)) }
        if (onShare != null && favorite != null) {
            add(
                BoomMenuAction(
                    stringResource(R.string.core_designsystem_share_with, favorite.label),
                    kind = BoomMenuKind.ShareWithFavorite,
                ) {
                    favorite.prepare()
                    onShare()
                },
            )
        }
        onYoutubeMusic?.let {
            add(BoomMenuAction(stringResource(R.string.core_designsystem_youtube_music), kind = BoomMenuKind.YoutubeMusic, onClick = it))
        }
        onDeezer?.let { add(BoomMenuAction(stringResource(R.string.core_designsystem_open_deezer), kind = BoomMenuKind.Deezer, onClick = it)) }
    }.inMenuOrder()

    BoomModalSheet(
        onDismissRequest = { open = false },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        // O painel, para fechar de dentro das colunas (que não deixam chamá-lo direto).
        val sheet = this
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            if (header != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 16.dp),
                ) {
                    BoomArtwork(
                        image = header.image,
                        fallbackText = header.title,
                        size = MenuHeaderArt,
                        modifier = if (header.round) Modifier.clip(CircleShape) else Modifier,
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(header.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        header.subtitle?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))
            }
            all.forEach { action ->
                val tint = if (action.kind == BoomMenuKind.Delete) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = action.enabled) {
                            // Fecha com a animação de descer e só então some de vez.
                            action.onClick()
                            sheet.dismiss()
                        }
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                        .alpha(if (action.enabled) 1f else 0.38f),
                ) {
                    val icon = action.icon ?: action.kind.icon
                    if (icon != null) {
                        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                    } else {
                        Spacer(Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(20.dp))
                    Text(
                        action.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (action.kind == BoomMenuKind.Delete) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private val MenuHeaderArt = 56.dp

/**
 * Toque normal faz [onClick]; segurar abre o menu de três pontinhos da linha
 * ([menuOpen], o mesmo estado passado ao [BoomMoreMenu]), com uma vibração curta.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.clickableWithMenu(
    menuOpen: MutableState<Boolean>,
    // Falso enquanto o dedo está num puxador de arrastar dentro da linha:
    // segurar ali é para reordenar, não para abrir o menu.
    longPressEnabled: () -> Boolean = { true },
    onClick: () -> Unit,
): Modifier {
    val haptics = LocalHapticFeedback.current
    return combinedClickable(
        onClick = onClick,
        onLongClick = {
            if (longPressEnabled()) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                menuOpen.value = true
            }
        },
    )
}

/** Marca [pressed] enquanto houver um dedo neste elemento (ex.: o puxador de arrastar). */
fun Modifier.trackPress(pressed: MutableState<Boolean>): Modifier = pointerInput(pressed) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        pressed.value = true
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
        } while (event.changes.any { it.pressed })
        pressed.value = false
    }
}

/**
 * Opções na ordem do menu: pelo tipo ([BoomMenuKind]); as do mesmo tipo
 * ficam na ordem em que a tela passou.
 */
internal fun List<BoomMenuAction>.inMenuOrder(): List<BoomMenuAction> = sortedBy { it.kind.ordinal }
