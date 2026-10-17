// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.social

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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

/** Perfil de outra pessoa: o que está ouvindo (se compartilha com você), conversar e bloquear. */
@Composable
internal fun FriendProfileScreen(
    viewModel: FriendProfileViewModel,
    onBack: () -> Unit,
    onOpenChat: (conversationId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
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
            is ProfileUiState.Ready -> LazyColumn(contentPadding = padding) {
                item(key = "header") {
                    Header(
                        profile = current.profile,
                        onMessage = { scope.launch { viewModel.openChat()?.let(onOpenChat) } },
                        onToggleBlock = viewModel::toggleBlock,
                    )
                }
                item(key = "now") { NowSection(current.profile) }
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
private fun Header(profile: FriendProfile, onMessage: () -> Unit, onToggleBlock: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(20.dp),
    ) {
        Avatar(profile.user.name, size = 96.dp)
        Spacer(Modifier.height(12.dp))
        Text(profile.user.name, style = MaterialTheme.typography.headlineSmall)
        Text("@" + profile.user.handle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onMessage, enabled = !profile.blockedByMe) {
                Icon(BoomIcons.Chat, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.feature_social_message))
            }
            TextButton(onClick = onToggleBlock) {
                Text(stringResource(if (profile.blockedByMe) R.string.feature_social_unblock else R.string.feature_social_block))
            }
        }
    }
}

@Composable
private fun NowSection(profile: FriendProfile) {
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
                }
            }
        }
    }
}
