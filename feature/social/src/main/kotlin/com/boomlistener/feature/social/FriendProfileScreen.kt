// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.social

import androidx.compose.foundation.layout.fillMaxSize
import com.boomlistener.core.designsystem.component.profileBackground
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import com.boomlistener.data.social.ProfileImage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.designsystem.component.BoomLoading
import com.boomlistener.core.designsystem.component.BoomPlaceholder
import com.boomlistener.core.designsystem.component.BoomScreenScaffold
import com.boomlistener.data.social.FriendProfile
import com.boomlistener.data.social.Relation

/**
 * Perfil de outra pessoa: redes (as públicas para todos; o número só para
 * amigos), amizade, o que está ouvindo (amigos que compartilham), reações e bloqueio.
 */
@Composable
internal fun FriendProfileScreen(
    viewModel: FriendProfileViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ready = (state as? ProfileUiState.Ready)?.profile

    BoomScreenScaffold(title = ready?.user?.name.orEmpty(), onBack = onBack, modifier = modifier) { padding ->
        when (val current = state) {
            ProfileUiState.Loading -> BoomLoading(Modifier.padding(padding))
            ProfileUiState.NotFound -> BoomPlaceholder(
                title = stringResource(R.string.feature_social_not_found),
                message = "",
                modifier = Modifier.padding(padding),
            )
            ProfileUiState.Offline -> BoomPlaceholder(
                title = stringResource(R.string.feature_social_offline),
                message = "",
                modifier = Modifier.padding(padding),
                actions = { TextButton(onClick = viewModel::load) { Text(stringResource(R.string.feature_social_retry)) } },
            )
            is ProfileUiState.Ready -> LazyColumn(
                contentPadding = padding,
                // A cor que a pessoa escolheu tinge o perfil dela.
                modifier = Modifier.fillMaxSize().profileBackground(current.profile.color),
            ) {
                item(key = "header") {
                    Header(
                        profile = current.profile,
                        onAdd = viewModel::addFriend,
                        onRemove = viewModel::removeFriend,
                        onToggleBlock = viewModel::toggleBlock,
                    )
                }
                if (current.profile.relation == Relation.Friend) {
                    item(key = "now") { NowSection(current.profile, onReact = viewModel::react) }
                }
                if (current.profile.recent.isNotEmpty()) {
                    item(key = "recent-title") {
                        Text(
                            stringResource(R.string.feature_social_recently),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp),
                        )
                    }
                    items(current.profile.recent, key = { it.atMs }) { item ->
                        ListItem(
                            leadingContent = { Icon(BoomIcons.MusicNote, contentDescription = null) },
                            headlineContent = { Text(item.track.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = {
                                Text(
                                    listOfNotNull(item.track.artist, relativeTime(item.atMs)).joinToString(" · "),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(
    profile: FriendProfile,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onToggleBlock: () -> Unit,
) {
    val banner = rememberProfileImage(profile.user.id, ProfileImage.Banner, profile.bannerVersion)
    val photo = rememberProfileImage(profile.user.id, ProfileImage.Avatar, profile.avatarVersion)
    var confirmUnfriend by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        // Capa no topo, com a foto encaixada na borda de baixo.
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp), contentAlignment = Alignment.BottomCenter) {
            ProfileBanner(banner, Modifier.padding(bottom = 48.dp).clip(MaterialTheme.shapes.extraLarge))
            Avatar(
                profile.user.name,
                size = 104.dp,
                picture = photo,
                modifier = Modifier.border(4.dp, MaterialTheme.colorScheme.surface, CircleShape),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(profile.user.name, style = MaterialTheme.typography.headlineSmall)
        Text("@" + profile.user.handle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (profile.bio.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                profile.bio,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        // Redes: a conversa acontece lá (o app não tem chat).
        NetworkButtons(links = profile.links, numbers = profile.numbers)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!profile.blockedByMe) {
                when (profile.relation) {
                    Relation.Friend -> OutlinedButton(onClick = { confirmUnfriend = true }) {
                        Icon(BoomIcons.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.feature_social_already_friends))
                    }
                    Relation.Sent -> OutlinedButton(onClick = onRemove) {
                        Text(stringResource(R.string.feature_social_cancel_request))
                    }
                    Relation.Received -> Button(onClick = onAdd) {
                        Text(stringResource(R.string.feature_social_accept))
                    }
                    Relation.None -> Button(onClick = onAdd) {
                        Icon(BoomIcons.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.feature_social_add_friend))
                    }
                    Relation.Self -> Unit
                }
            }
            if (profile.relation != Relation.Self) {
                TextButton(onClick = onToggleBlock) {
                    Text(stringResource(if (profile.blockedByMe) R.string.feature_social_unblock else R.string.feature_social_block))
                }
            }
        }
        if (profile.relation == Relation.Received) {
            TextButton(onClick = onRemove) { Text(stringResource(R.string.feature_social_decline)) }
        }
        Spacer(Modifier.height(16.dp))
    }
    if (confirmUnfriend) {
        AlertDialog(
            onDismissRequest = { confirmUnfriend = false },
            title = { Text(stringResource(R.string.feature_social_unfriend_title, profile.user.name)) },
            text = { Text(stringResource(R.string.feature_social_unfriend_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmUnfriend = false
                    onRemove()
                }) { Text(stringResource(R.string.feature_social_unfriend)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmUnfriend = false }) { Text(stringResource(R.string.feature_social_cancel)) }
            },
        )
    }
}

@Composable
private fun NowSection(profile: FriendProfile, onReact: suspend (String) -> Boolean) {
    val now = profile.now
    Surface(
        color = if (now != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (now != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            when {
                !profile.sharing -> Text(stringResource(R.string.feature_social_not_sharing), style = MaterialTheme.typography.bodyMedium)
                now == null -> Text(stringResource(R.string.feature_social_not_playing), style = MaterialTheme.typography.bodyMedium)
                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LiveDot()
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.feature_social_now), style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(now.title, style = MaterialTheme.typography.titleLarge)
                    Text(listOfNotNull(now.artist, now.album).joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    ReactionRow(onReact)
                }
            }
        }
    }
}
