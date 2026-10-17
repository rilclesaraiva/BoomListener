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
import com.boomlistener.data.social.SocialData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * "Recomendar para…": escolhe uma conversa e manda o cartão da música
 * [trackUri] (só a informação, nunca o arquivo). Pública porque o :app usa
 * nos menus da biblioteca e do player.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendSheet(trackUri: String, onDismiss: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val social = remember { SocialData.repository(context) }
    val account by social.account.collectAsStateWithLifecycle()
    val conversations by social.conversations.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var sentTo by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }
    val effects = boomEffectsSpec<Float>()

    LaunchedEffect(sentTo) {
        if (sentTo != null) {
            delay(700)
            sheetState.hide()
            onDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text(
            stringResource(R.string.feature_social_recommend_to),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        if (account == null || conversations.isEmpty()) {
            Text(
                stringResource(if (account == null) R.string.feature_social_recommend_no_profile else R.string.feature_social_no_conversations),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            )
        }
        LazyColumn(Modifier.padding(bottom = 24.dp)) {
            items(conversations, key = { it.id }) { conversation ->
                ListItem(
                    modifier = Modifier.clickable(enabled = !sending) {
                        sending = true
                        scope.launch {
                            val track = LibraryData.repository(context).getTracks().firstOrNull { it.uri == trackUri }
                            if (track != null) {
                                social.send(conversation.id, buildTrackCard(track, DeezerData.repository(context)))
                            }
                            sentTo = conversation.id
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = {
                        AnimatedContent(
                            targetState = sentTo == conversation.id,
                            transitionSpec = { fadeIn(effects) togetherWith fadeOut(effects) },
                            label = "sent",
                        ) { sent ->
                            if (sent) Icon(BoomIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            else Avatar(conversation.title, isGroup = conversation.isGroup, size = 40.dp)
                        }
                    },
                    headlineContent = { Text(conversation.title.ifBlank { stringResource(R.string.feature_social_group) }) },
                )
            }
        }
    }
}
