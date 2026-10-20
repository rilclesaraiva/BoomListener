// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.social

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.core.common.normalizedForSearch
import com.boomlistener.core.common.openUrl
import com.boomlistener.core.common.shareText
import com.boomlistener.core.designsystem.component.BoomArtwork
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.designsystem.component.BoomMenuAction
import com.boomlistener.core.designsystem.component.BoomMoreMenu
import com.boomlistener.core.designsystem.component.BoomScreenScaffold
import com.boomlistener.core.model.Track
import com.boomlistener.data.social.ChatMessage
import com.boomlistener.data.social.Delivery
import com.boomlistener.data.social.MessageBody
import kotlinx.coroutines.launch

@Composable
internal fun ChatScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    onOpenProfile: (userId: String) -> Unit,
    onOpenArtist: (artistName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val conversation by viewModel.conversation.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val members by viewModel.members.collectAsStateWithLifecycle()
    val sending by viewModel.sending.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var picking by remember { mutableStateOf(false) }
    var showMembers by remember { mutableStateOf(false) }
    var confirmLeave by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val isGroup = viewModel.conversationId.startsWith("g:")

    DisposableEffect(viewModel) {
        viewModel.opened()
        onDispose { viewModel.closed() }
    }
    // Mensagens chegam ao vivo pela conexão; sem ela, consulta de vez em quando.
    PollWhileVisible(30_000) { viewModel.sync() }
    // Mensagem nova: a lista (de baixo para cima) volta para a mais recente.
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(0) }

    val inviteText = conversation?.code?.let { stringResource(R.string.feature_social_invite_text, conversation?.title.orEmpty(), it) }

    BoomScreenScaffold(
        title = conversation?.title?.ifBlank { null } ?: stringResource(R.string.feature_social_group),
        onBack = onBack,
        modifier = modifier,
        actions = {
            if (isGroup) {
                BoomMoreMenu(
                    actions = listOfNotNull(
                        inviteText?.let { invite ->
                            BoomMenuAction(stringResource(R.string.feature_social_invite)) { context.shareText(invite) }
                        },
                        BoomMenuAction(stringResource(R.string.feature_social_members)) {
                            viewModel.refreshMembers()
                            showMembers = true
                        },
                        BoomMenuAction(stringResource(R.string.feature_social_leave_group)) { confirmLeave = true },
                    ),
                )
            } else {
                conversation?.otherUserId?.let { userId ->
                    IconButton(onClick = { onOpenProfile(userId) }) {
                        Icon(BoomIcons.Person, contentDescription = stringResource(R.string.feature_social_profile))
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (messages.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            text = stringResource(
                                if (isGroup) R.string.feature_social_group_empty else R.string.feature_social_chat_empty,
                                conversation?.code.orEmpty(),
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp),
                        )
                    }
                }
                items(messages.asReversed(), key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        showSender = isGroup,
                        viewModel = viewModel,
                        onOpenArtist = onOpenArtist,
                        onOpenLink = context::openUrl,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            InputBar(
                text = text,
                onTextChange = { text = it },
                sending = sending,
                onSend = {
                    viewModel.sendText(text)
                    text = ""
                },
                onPickMusic = { picking = true },
            )
        }
    }

    if (picking) {
        TrackPickerSheet(
            viewModel = viewModel,
            onPick = { track ->
                picking = false
                viewModel.sendTrack(track, note = text)
                text = ""
            },
            onDismiss = { picking = false },
        )
    }
    if (showMembers) {
        MembersSheet(viewModel, onOpenProfile = { showMembers = false; onOpenProfile(it) }, onDismiss = { showMembers = false })
    }
    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text(stringResource(R.string.feature_social_leave_group_title)) },
            text = { Text(stringResource(R.string.feature_social_leave_group_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmLeave = false
                    scope.launch { if (viewModel.leave()) onBack() }
                }) { Text(stringResource(R.string.feature_social_leave)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmLeave = false }) { Text(stringResource(R.string.feature_social_cancel)) }
            },
        )
    }
}

@Composable
private fun InputBar(
    text: String,
    onTextChange: (String) -> Unit,
    sending: Boolean,
    onSend: () -> Unit,
    onPickMusic: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            IconButton(onClick = onPickMusic, enabled = !sending) {
                Icon(BoomIcons.MusicNote, contentDescription = stringResource(R.string.feature_social_recommend))
            }
            OutlinedTextField(
                value = text,
                onValueChange = { onTextChange(it.take(2000)) },
                placeholder = { Text(stringResource(if (sending) R.string.feature_social_sending else R.string.feature_social_type)) },
                maxLines = 5,
                shape = MaterialTheme.shapes.extraLarge,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.size(8.dp))
            FilledIconButton(onClick = onSend, enabled = text.isNotBlank()) {
                Icon(BoomIcons.Send, contentDescription = stringResource(R.string.feature_social_send))
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    showSender: Boolean,
    viewModel: ChatViewModel,
    onOpenArtist: (String) -> Unit,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val mine = message.sender == null
    Row(
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp),
    ) {
        Surface(
            color = if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = if (mine) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                val sender = message.sender
                if (showSender && sender != null) {
                    Text(sender.name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                when (val body = message.body) {
                    is MessageBody.Text -> Text(body.text, style = MaterialTheme.typography.bodyLarge)
                    is MessageBody.Music -> MusicCard(body, viewModel, onOpenArtist, onOpenLink)
                }
                Text(
                    text = deliveryLabel(message),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                )
            }
        }
    }
}

/** Cartão de música: capa, nome e o que dá para fazer (tocar se você tem, ouvir fora, ver o artista). */
@Composable
private fun MusicCard(
    card: MessageBody.Music,
    viewModel: ChatViewModel,
    onOpenArtist: (String) -> Unit,
    onOpenLink: (String) -> Unit,
) {
    val px = with(LocalDensity.current) { 56.dp.roundToPx() }
    val cover by produceState<ImageBitmap?>(initialValue = null, card.coverUrl) {
        value = card.coverUrl?.let { viewModel.image(it, px)?.asImageBitmap() }
    }
    val localTracks by viewModel.localTracks.collectAsStateWithLifecycle()
    val local = remember(card, localTracks) { viewModel.localMatch(card) }
    Column {
        card.note?.let {
            Text(it, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            BoomArtwork(image = cover, fallbackText = card.title, size = 56.dp)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f, fill = false)) {
                Text(card.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(card.artist, card.album).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row {
            if (local != null) {
                TextButton(onClick = { viewModel.play(card) }) { Text(stringResource(R.string.feature_social_play)) }
            }
            card.link?.let { link ->
                TextButton(onClick = { onOpenLink(link) }) { Text(stringResource(R.string.feature_social_listen_outside)) }
            }
            card.artist?.let { artist ->
                TextButton(onClick = { onOpenArtist(artist) }) { Text(stringResource(R.string.feature_social_artist)) }
            }
        }
    }
}

/** Escolher uma música do aparelho para recomendar (vai só a informação). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackPickerSheet(viewModel: ChatViewModel, onPick: (Track) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tracks by viewModel.localTracks.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val filtered = remember(tracks, query) {
        val wanted = query.normalizedForSearch()
        if (wanted.isEmpty()) tracks else tracks.filter { track ->
            listOfNotNull(track.title, track.artist, track.album).any { it.normalizedForSearch().contains(wanted) }
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text(
            stringResource(R.string.feature_social_recommend),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.feature_social_search_songs)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        )
        LazyColumn(Modifier.imePadding()) {
            items(filtered.take(300), key = { it.id }) { track ->
                PickerRow(track, viewModel, onClick = { onPick(track) })
            }
        }
    }
}

@Composable
private fun PickerRow(track: Track, viewModel: ChatViewModel, onClick: () -> Unit) {
    val px = with(LocalDensity.current) { 48.dp.roundToPx() }
    val artwork by produceState<ImageBitmap?>(initialValue = null, track.uri) {
        value = viewModel.artwork(track.uri, px)?.asImageBitmap()
    }
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { BoomArtwork(image = artwork, fallbackText = track.title, size = 48.dp) },
        headlineContent = { Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(track.artist.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MembersSheet(viewModel: ChatViewModel, onOpenProfile: (String) -> Unit, onDismiss: () -> Unit) {
    val members by viewModel.members.collectAsStateWithLifecycle()
    val conversation by viewModel.conversation.collectAsStateWithLifecycle()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            stringResource(R.string.feature_social_members),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        conversation?.code?.let { code ->
            Text(
                stringResource(R.string.feature_social_group_code_is, code),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )
        }
        LazyColumn(Modifier.padding(bottom = 24.dp)) {
            items(members, key = { it.id }) { member ->
                ListItem(
                    modifier = Modifier.clickable { onOpenProfile(member.id) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = { Avatar(member.name) },
                    headlineContent = { Text(member.name) },
                    supportingContent = { Text("@" + member.handle) },
                )
            }
        }
    }
}

/** Hora da mensagem; nas suas, também onde ela está (aguardando, entregue, caixa postal). */
@Composable
private fun deliveryLabel(message: ChatMessage): String {
    val time = relativeTime(message.sentMs)
    if (message.sender != null) return time
    return when (message.delivery) {
        Delivery.Waiting -> stringResource(R.string.feature_social_delivery_waiting)
        Delivery.Delivered -> stringResource(R.string.feature_social_delivery_delivered, time)
        Delivery.Mailbox -> stringResource(R.string.feature_social_delivery_mailbox, time)
        Delivery.Sent -> time
    }
}
