// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.app

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.boomlistener.app.navigation.TopLevelDestination
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.designsystem.theme.boomSpatialSpec
import com.boomlistener.feature.releases.FollowedArtistsList

/** Tela larga: tablet, ou celular deitado com largura de sobra. */
internal fun isWideWindow(width: Dp, height: Dp): Boolean =
    width >= 840.dp || (width >= 600.dp && width > height)

private val SidebarExpanded = 264.dp
private val SidebarCollapsed = 80.dp

/**
 * Barra lateral das telas largas (no lugar da barra de abas): Lançamentos,
 * Início e Biblioteca, "Nova playlist", Favoritos e os artistas que você
 * segue. [expanded] falso recolhe para só os ícones e as fotos.
 */
@Composable
internal fun WideSidebar(
    expanded: Boolean,
    onToggle: () -> Unit,
    selected: TopLevelDestination?,
    onOpenTab: (TopLevelDestination) -> Unit,
    onOpenReleases: () -> Unit,
    onNewPlaylist: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenArtist: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val width by animateDpAsState(if (expanded) SidebarExpanded else SidebarCollapsed, boomSpatialSpec(), label = "sidebarWidth")
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = if (expanded) Alignment.Start else Alignment.CenterHorizontally,
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical))
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        // Lançamentos no topo, em destaque.
        if (expanded) {
            FilledTonalButton(onClick = onOpenReleases, modifier = Modifier.fillMaxWidth()) {
                Icon(BoomIcons.Releases, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.wide_releases))
            }
        } else {
            IconButton(onClick = onToggle) {
                Icon(BoomIcons.ChevronRight, contentDescription = stringResource(R.string.wide_expand))
            }
            FilledTonalIconButton(onClick = onOpenReleases) {
                Icon(BoomIcons.Releases, contentDescription = stringResource(R.string.wide_releases))
            }
        }
        Spacer(Modifier.size(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SidebarItem(
                destination = TopLevelDestination.Home,
                selected = selected == TopLevelDestination.Home,
                expanded = expanded,
                onClick = { onOpenTab(TopLevelDestination.Home) },
                modifier = if (expanded) Modifier.weight(1f) else Modifier,
            )
            // Recolher fica ao lado de Início (como no rascunho).
            if (expanded) {
                IconButton(onClick = onToggle) {
                    Icon(BoomIcons.ChevronLeft, contentDescription = stringResource(R.string.wide_collapse))
                }
            }
        }
        SidebarItem(
            destination = TopLevelDestination.Collections,
            selected = selected == TopLevelDestination.Collections,
            expanded = expanded,
            onClick = { onOpenTab(TopLevelDestination.Collections) },
        )
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        SidebarPill(BoomIcons.Add, stringResource(R.string.wide_new_playlist), expanded, outlined = true, onClick = onNewPlaylist)
        SidebarPill(BoomIcons.Favorite, stringResource(R.string.wide_favorites), expanded, outlined = false, onClick = onOpenFavorites)
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        // Os artistas que você segue, numa lista que rola.
        FollowedArtistsList(
            collapsed = !expanded,
            onOpenArtist = onOpenArtist,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
    }
}

@Composable
private fun SidebarItem(
    destination: TopLevelDestination,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(destination.label)
    NavigationDrawerItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(destination.icon, contentDescription = if (expanded) null else label) },
        label = { if (expanded) Text(label) },
        modifier = if (expanded) modifier.fillMaxWidth() else modifier.width(56.dp),
    )
}

/** "Nova playlist" e "Favoritos": pílulas (ou só o ícone, recolhida). */
@Composable
private fun SidebarPill(icon: ImageVector, label: String, expanded: Boolean, outlined: Boolean, onClick: () -> Unit) {
    if (expanded) {
        val content: @Composable () -> Unit = {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label)
        }
        if (outlined) {
            OutlinedButton(onClick = onClick) { content() }
        } else {
            FilledTonalButton(onClick = onClick) { content() }
        }
    } else if (outlined) {
        OutlinedIconButton(onClick = onClick) { Icon(icon, contentDescription = label) }
    } else {
        FilledTonalIconButton(onClick = onClick) { Icon(icon, contentDescription = label) }
    }
}
