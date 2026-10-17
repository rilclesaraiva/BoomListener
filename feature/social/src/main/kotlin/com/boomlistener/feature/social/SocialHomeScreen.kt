// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.social

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.designsystem.component.BoomScreenScaffold
import com.boomlistener.core.designsystem.component.BoomTopActions
import com.boomlistener.core.designsystem.component.ScrollToTopOnReselect
import com.boomlistener.core.designsystem.theme.boomEffectsSpec
import com.boomlistener.data.social.ChatMessage
import com.boomlistener.data.social.Conversation
import com.boomlistener.data.social.FriendProfile
import com.boomlistener.data.social.HandleCheck
import com.boomlistener.data.social.MessageBody
import com.boomlistener.data.social.SocialResult
import com.boomlistener.data.social.isValidHandle
import com.boomlistener.data.social.suggestHandle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Aba Amigos. Sem perfil, mostra a criação do perfil. */
@Composable
internal fun SocialHomeScreen(
    viewModel: SocialHomeViewModel,
    onOpenChat: (conversationId: String) -> Unit,
    onOpenProfile: (userId: String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val account by viewModel.account.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val activity by viewModel.activity.collectAsStateWithLifecycle()
    val offline by viewModel.offline.collectAsStateWithLifecycle()
    var newConversation by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    ScrollToTopOnReselect(SOCIAL_ROUTE, listState)
    if (account != null) PollWhileVisible(15_000) { viewModel.refresh() }

    BoomScreenScaffold(
        title = stringResource(R.string.feature_social_title),
        modifier = modifier,
        actions = { BoomTopActions(onOpenHistory = onOpenHistory, onOpenSettings = onOpenSettings) },
    ) { padding ->
        val effects = boomEffectsSpec<Float>()
        AnimatedContent(
            targetState = account == null,
            transitionSpec = { fadeIn(effects) togetherWith fadeOut(effects) },
            label = "onboarding",
            modifier = Modifier.padding(padding),
        ) { needsProfile ->
            if (needsProfile) {
                Onboarding(viewModel)
            } else {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
                    LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 96.dp)) {
                        account?.let { me ->
                            item(key = "me") {
                                Text(
                                    text = stringResource(
                                        if (me.sharing) R.string.feature_social_me_sharing else R.string.feature_social_me_private,
                                        me.handle,
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                                )
                            }
                        }
                        if (offline) {
                            item(key = "offline") {
                                Text(
                                    text = stringResource(R.string.feature_social_offline),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                                )
                            }
                        }
                        if (activity.isNotEmpty()) {
                            item(key = "activity") {
                                SectionTitle(stringResource(R.string.feature_social_listening_now))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                ) {
                                    items(activity, key = { it.user.id }) { friend ->
                                        ActivityCard(friend, onClick = { onOpenProfile(friend.user.id) })
                                    }
                                }
                            }
                        }
                        item(key = "chats-title") { SectionTitle(stringResource(R.string.feature_social_conversations)) }
                        if (conversations.isEmpty()) {
                            item(key = "empty") {
                                Text(
                                    text = stringResource(R.string.feature_social_no_conversations),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                                )
                            }
                        }
                        items(conversations, key = { it.id }) { conversation ->
                            ConversationRow(conversation, onClick = { onOpenChat(conversation.id) }, modifier = Modifier.animateItem())
                        }
                    }
                    ExtendedFloatingActionButton(
                        onClick = { newConversation = true },
                        icon = { Icon(BoomIcons.Add, contentDescription = null) },
                        text = { Text(stringResource(R.string.feature_social_new_conversation)) },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                    )
                }
            }
        }
    }

    if (newConversation) {
        NewConversationSheet(
            viewModel = viewModel,
            onOpenChat = { id ->
                newConversation = false
                onOpenChat(id)
            },
            onDismiss = { newConversation = false },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
    )
}

/** Cartão "ouvindo agora" (ou a última que ouviu) de um amigo. */
@Composable
private fun ActivityCard(friend: FriendProfile, onClick: () -> Unit) {
    val track = friend.now ?: friend.recent.firstOrNull()?.track
    val live = friend.now != null && (friend.updatedMs ?: 0) > System.currentTimeMillis() - 15 * 60_000
    Surface(
        color = if (live) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (live) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.width(200.dp).clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(friend.user.name, size = 32.dp)
                Spacer(Modifier.width(8.dp))
                Text(friend.user.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(if (live) R.string.feature_social_now else R.string.feature_social_last_heard),
                style = MaterialTheme.typography.labelSmall,
            )
            Text(track?.title.orEmpty(), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track?.artist.orEmpty(), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ConversationRow(conversation: Conversation, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        leadingContent = { Avatar(conversation.title, isGroup = conversation.isGroup) },
        headlineContent = {
            Text(
                conversation.title.ifBlank { stringResource(R.string.feature_social_group) },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            conversation.lastMessage?.let { last ->
                Text(preview(last), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                conversation.lastMessage?.let {
                    Text(relativeTime(it.sentMs), style = MaterialTheme.typography.labelSmall)
                }
                if (conversation.unread > 0) {
                    Spacer(Modifier.height(4.dp))
                    Badge { Text(conversation.unread.toString()) }
                }
            }
        },
    )
}

/** Uma linha que resume a mensagem: o texto, ou "🎵 Música — Artista". */
@Composable
internal fun preview(message: ChatMessage): String {
    val sender = message.sender
    val prefix = when {
        sender == null -> stringResource(R.string.feature_social_you) + ": "
        message.conversationId.startsWith("g:") -> sender.name + ": "
        else -> ""
    }
    return prefix + when (val body = message.body) {
        is MessageBody.Text -> body.text
        is MessageBody.Music -> "♪ " + listOfNotNull(body.title, body.artist).joinToString(" — ")
    }
}

/** Criar o perfil: nome, @ (conferido na hora) e se quer compartilhar o que ouve. */
@Composable
private fun Onboarding(viewModel: SocialHomeViewModel) {
    var name by remember { mutableStateOf("") }
    var handle by remember { mutableStateOf("") }
    var handleTouched by remember { mutableStateOf(false) }
    var share by remember { mutableStateOf(false) }
    var check by remember { mutableStateOf<HandleCheck?>(null) }
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    // Sugere o @ a partir do nome, até a pessoa mexer nele.
    LaunchedEffect(name) { if (!handleTouched && name.isNotBlank()) handle = suggestHandle(name) }
    // Confere se o @ está livre, esperando a pessoa parar de digitar.
    LaunchedEffect(handle) {
        check = null
        if (!isValidHandle(handle)) return@LaunchedEffect
        delay(500)
        check = viewModel.checkHandle(handle)
    }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.feature_social_onboarding_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.feature_social_onboarding_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(40) },
            label = { Text(stringResource(R.string.feature_social_name)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = handle,
            onValueChange = {
                handleTouched = true
                handle = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' || c == '.' }.take(20)
            },
            label = { Text(stringResource(R.string.feature_social_handle)) },
            prefix = { Text("@") },
            singleLine = true,
            supportingText = {
                Text(
                    stringResource(
                        when {
                            handle.isNotEmpty() && !isValidHandle(handle) -> R.string.feature_social_handle_rules
                            check == HandleCheck.Available -> R.string.feature_social_handle_available
                            check == HandleCheck.Taken -> R.string.feature_social_handle_taken
                            check == HandleCheck.Offline -> R.string.feature_social_offline
                            else -> R.string.feature_social_handle_rules
                        },
                    ),
                    color = if (check == HandleCheck.Available) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            isError = check == HandleCheck.Taken,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.feature_social_share_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.feature_social_share_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = share, onCheckedChange = { share = it })
        }
        error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = {
                working = true
                error = null
                scope.launch {
                    val result = viewModel.register(name, handle, share)
                    working = false
                    error = when (result) {
                        is SocialResult.Ok -> null
                        SocialResult.HandleTaken -> R.string.feature_social_handle_taken
                        SocialResult.Offline -> R.string.feature_social_offline
                        else -> R.string.feature_social_error
                    }
                }
            },
            enabled = !working && name.isNotBlank() && isValidHandle(handle) && check != HandleCheck.Taken,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(if (working) R.string.feature_social_creating else R.string.feature_social_create_profile))
        }
        Text(
            stringResource(R.string.feature_social_onboarding_privacy),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private enum class NewMode { Person, CreateGroup, JoinGroup }

/** Nova conversa: procurar alguém pelo @, criar um grupo ou entrar com um código. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewConversationSheet(
    viewModel: SocialHomeViewModel,
    onOpenChat: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf<NewMode?>(null) }
    var input by remember { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Int?>(null) }
    var found by remember { mutableStateOf<FriendProfile?>(null) }
    val effects = boomEffectsSpec<Float>()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        AnimatedContent(
            targetState = mode,
            transitionSpec = { fadeIn(effects) togetherWith fadeOut(effects) },
            label = "newMode",
            modifier = Modifier.imePadding(),
        ) { current ->
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
                if (current == null) {
                    Text(stringResource(R.string.feature_social_new_conversation), style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        NewMode.Person to R.string.feature_social_new_person,
                        NewMode.CreateGroup to R.string.feature_social_new_group,
                        NewMode.JoinGroup to R.string.feature_social_join_group,
                    ).forEach { (option, label) ->
                        ListItem(
                            modifier = Modifier.clickable {
                                mode = option
                                input = ""
                                error = null
                                found = null
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            leadingContent = {
                                Icon(if (option == NewMode.Person) BoomIcons.Person else BoomIcons.Add, contentDescription = null)
                            },
                            headlineContent = { Text(stringResource(label)) },
                        )
                    }
                } else {
                    Text(
                        stringResource(
                            when (current) {
                                NewMode.Person -> R.string.feature_social_new_person
                                NewMode.CreateGroup -> R.string.feature_social_new_group
                                NewMode.JoinGroup -> R.string.feature_social_join_group
                            },
                        ),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = input,
                        onValueChange = {
                            input = when (current) {
                                NewMode.Person -> it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' || c == '.' }.take(20)
                                NewMode.JoinGroup -> it.uppercase().filter { c -> c.isLetterOrDigit() }.take(8)
                                NewMode.CreateGroup -> it.take(40)
                            }
                            found = null
                            error = null
                        },
                        label = {
                            Text(
                                stringResource(
                                    when (current) {
                                        NewMode.Person -> R.string.feature_social_handle
                                        NewMode.CreateGroup -> R.string.feature_social_group_name
                                        NewMode.JoinGroup -> R.string.feature_social_group_code
                                    },
                                ),
                            )
                        },
                        prefix = if (current == NewMode.Person) {
                            { Text("@") }
                        } else {
                            null
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    error?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    found?.let { profile ->
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            leadingContent = { Avatar(profile.user.name) },
                            headlineContent = { Text(profile.user.name) },
                            supportingContent = { Text("@" + profile.user.handle) },
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { mode = null }) { Text(stringResource(R.string.feature_social_back)) }
                        Spacer(Modifier.weight(1f))
                        FilledTonalButton(
                            enabled = !working && input.isNotBlank(),
                            onClick = {
                                working = true
                                error = null
                                scope.launch {
                                    when (current) {
                                        NewMode.Person -> {
                                            val profile = found
                                            if (profile != null) {
                                                onOpenChat(viewModel.openDirect(profile.user))
                                            } else {
                                                when (val result = viewModel.findUser(input)) {
                                                    is SocialResult.Ok -> found = result.value
                                                    SocialResult.Offline -> error = R.string.feature_social_offline
                                                    else -> error = R.string.feature_social_not_found
                                                }
                                            }
                                        }
                                        NewMode.CreateGroup -> when (val result = viewModel.createGroup(input)) {
                                            is SocialResult.Ok -> onOpenChat(result.value.id)
                                            SocialResult.Offline -> error = R.string.feature_social_offline
                                            else -> error = R.string.feature_social_error
                                        }
                                        NewMode.JoinGroup -> when (val result = viewModel.joinGroup(input)) {
                                            is SocialResult.Ok -> onOpenChat(result.value.id)
                                            SocialResult.Offline -> error = R.string.feature_social_offline
                                            else -> error = R.string.feature_social_group_not_found
                                        }
                                    }
                                    working = false
                                }
                            },
                        ) {
                            Text(
                                stringResource(
                                    when {
                                        current == NewMode.Person && found != null -> R.string.feature_social_message
                                        current == NewMode.Person -> R.string.feature_social_search
                                        current == NewMode.CreateGroup -> R.string.feature_social_create
                                        else -> R.string.feature_social_join
                                    },
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Um ponto colorido pequeno (usado como "ao vivo"). */
@Composable
internal fun LiveDot() {
    Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape, modifier = Modifier.size(8.dp)) {}
}
