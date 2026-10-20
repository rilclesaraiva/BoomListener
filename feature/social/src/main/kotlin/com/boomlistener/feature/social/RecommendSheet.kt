// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.social

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.designsystem.theme.boomEffectsSpec
import com.boomlistener.data.deezer.DeezerData
import com.boomlistener.data.library.LibraryData
import com.boomlistener.data.social.ProfileImage
import com.boomlistener.data.social.SocialData
import com.boomlistener.data.social.SocialResult
import kotlinx.coroutines.launch

/**
 * "Recomendar para…": um toque num amigo e ele recebe o cartão da música
 * [trackUri] (capa, artista e link; nunca o arquivo). Pública porque o :app
 * usa nos menus da biblioteca e do player.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendSheet(trackUri: String, onDismiss: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val social = remember { SocialData.repository(context) }
    val account by social.account.collectAsStateWithLifecycle()
    val friendsState by social.friends.collectAsStateWithLifecycle()
    val friends = remember(friendsState) { friendsState.friends.sortedBy { it.user.name.lowercase() } }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Quem já recebeu (marcado com ✓); dá para mandar para vários.
    var sentTo by remember { mutableStateOf(setOf<String>()) }
    var sending by remember { mutableStateOf<String?>(null) }
    var failed by remember { mutableStateOf(false) }
    val effects = boomEffectsSpec<Float>()

    LaunchedEffect(account?.id) { if (account != null) social.refreshFriends() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text(
            stringResource(R.string.feature_social_recommend_to),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        if (account == null || friends.isEmpty()) {
            Text(
                stringResource(if (account == null) R.string.feature_social_recommend_no_profile else R.string.feature_social_recommend_no_friends),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            )
        }
        if (failed) {
            Text(
                stringResource(R.string.feature_social_offline),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
        LazyColumn(Modifier.padding(bottom = 24.dp)) {
            items(friends, key = { it.user.id }) { friend ->
                val id = friend.user.id
                ListItem(
                    modifier = Modifier.clickable(enabled = sending == null && id !in sentTo) {
                        sending = id
                        failed = false
                        scope.launch {
                            val track = LibraryData.repository(context).getTracks().firstOrNull { it.uri == trackUri }
                            val ok = track != null &&
                                social.recommend(listOf(id), buildTrackCard(track, DeezerData.repository(context))) is SocialResult.Ok
                            if (ok) sentTo = sentTo + id else failed = true
                            sending = null
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = {
                        AnimatedContent(
                            targetState = id in sentTo,
                            transitionSpec = { fadeIn(effects) togetherWith fadeOut(effects) },
                            label = "sent",
                        ) { sent ->
                            if (sent) {
                                Icon(BoomIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            } else {
                                Avatar(
                                    friend.user.name,
                                    size = 40.dp,
                                    picture = rememberProfileImage(id, ProfileImage.Avatar, friend.avatarVersion),
                                )
                            }
                        }
                    },
                    headlineContent = { Text(friend.user.name) },
                    supportingContent = { Text("@" + friend.user.handle) },
                )
            }
        }
    }
}
