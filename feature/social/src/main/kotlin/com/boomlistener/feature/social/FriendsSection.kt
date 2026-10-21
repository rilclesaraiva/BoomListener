// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.social

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.core.common.openUrl
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.designsystem.component.BoomMenuAction
import com.boomlistener.core.designsystem.component.BoomMoreMenu
import com.boomlistener.core.designsystem.theme.boomEffectsSpec
import com.boomlistener.data.social.FriendProfile
import com.boomlistener.data.social.FriendsState
import com.boomlistener.data.social.HandleCheck
import com.boomlistener.data.social.ProfileImage
import com.boomlistener.data.social.REACTION_EMOJIS
import com.boomlistener.data.social.Recommendation
import com.boomlistener.data.social.Relation
import com.boomlistener.data.social.SocialResult
import com.boomlistener.data.social.SocialUser
import com.boomlistener.data.social.isValidHandle
import com.boomlistener.data.social.suggestHandle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** "Ao vivo" = tocou ou mudou de música nos últimos 15 minutos. */
private const val LIVE_WINDOW_MS = 15 * 60_000L

/**
 * Amigos dentro do seu perfil (não existe mais uma aba só para isso nem chat):
 * pedidos de amizade, "O que seus amigos estão ouvindo" (com reações) e as
 * músicas que eles recomendaram. Quem chama já coletou os estados.
 */
internal fun LazyListScope.friendsSection(
    viewModel: SocialHomeViewModel,
    friends: List<FriendProfile>,
    requests: FriendsState,
    recommendations: List<Recommendation>,
    offline: Boolean,
    restingUntil: Long?,
    onOpenProfile: (userId: String) -> Unit,
    onAddFriend: () -> Unit,
) {
    restingUntil?.let { until ->
        item(key = "resting") { RestingNotice(until) }
    }
    if (offline && restingUntil == null) {
        item(key = "offline") {
            Text(
                text = stringResource(R.string.feature_social_offline),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }
    }
    if (requests.incoming.isNotEmpty()) {
        item(key = "requests-title") { SectionTitle(stringResource(R.string.feature_social_requests)) }
        items(requests.incoming, key = { "in-" + it.id }) { user ->
            RequestRow(
                user = user,
                onOpen = { onOpenProfile(user.id) },
                primary = stringResource(R.string.feature_social_accept) to { viewModel.addFriend(user.id) },
                secondary = stringResource(R.string.feature_social_decline) to { viewModel.removeFriend(user.id) },
                modifier = Modifier.animateItem(),
            )
        }
    }
    item(key = "friends-title") {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.weight(1f)) { SectionTitle(stringResource(R.string.feature_social_friends_listening)) }
            TextButton(onClick = onAddFriend, modifier = Modifier.padding(end = 8.dp, top = 8.dp)) {
                Icon(BoomIcons.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.feature_social_add_friend))
            }
        }
    }
    if (friends.isEmpty()) {
        item(key = "friends-empty") {
            Text(
                text = stringResource(R.string.feature_social_friends_none),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
    }
    if (friends.isNotEmpty()) {
        // Um cartão por amigo, numa fila que desliza para o lado.
        item(key = "friends-row") {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) {
                items(friends, key = { "f-" + it.user.id }) { friend ->
                    FriendCard(
                        friend = friend,
                        loadImage = viewModel::image,
                        onClick = { onOpenProfile(friend.user.id) },
                        onReact = { emoji -> viewModel.react(friend, emoji) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
    if (recommendations.isNotEmpty()) {
        item(key = "recs-title") { SectionTitle(stringResource(R.string.feature_social_recommendations)) }
        items(recommendations, key = { "r-" + it.id }) { rec ->
            RecommendationRow(rec, viewModel, onOpenProfile, Modifier.animateItem())
        }
    }
    if (requests.outgoing.isNotEmpty()) {
        item(key = "sent-title") { SectionTitle(stringResource(R.string.feature_social_requests_sent)) }
        items(requests.outgoing, key = { "out-" + it.id }) { user ->
            RequestRow(
                user = user,
                onOpen = { onOpenProfile(user.id) },
                primary = null,
                secondary = stringResource(R.string.feature_social_cancel_request) to { viewModel.removeFriend(user.id) },
                modifier = Modifier.animateItem(),
            )
        }
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

/** Um pedido de amizade (recebido: aceitar/recusar; enviado: cancelar). */
@Composable
private fun RequestRow(
    user: SocialUser,
    onOpen: () -> Unit,
    primary: Pair<String, () -> Unit>?,
    secondary: Pair<String, () -> Unit>,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier.clickable(onClick = onOpen),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { Avatar(user.name, size = 40.dp) },
        headlineContent = { Text(user.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text("@" + user.handle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = secondary.second) { Text(secondary.first) }
                primary?.let { (label, action) -> FilledTonalButton(onClick = action) { Text(label) } }
            }
        },
    )
}

/**
 * Cartão de um amigo: a capa do que está ouvindo agora (ou ouviu por último,
 * e quando) e as reações com emoji, quando ele está ouvindo. A capa vem do
 * Deezer (o amigo manda o endereço junto com a música).
 */
@Composable
internal fun FriendCard(
    friend: FriendProfile,
    loadImage: suspend (String, Int) -> Bitmap?,
    onClick: () -> Unit,
    onReact: suspend (String) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val track = friend.lastTrack.takeIf { friend.sharing }
    val live = friend.now != null && (friend.updatedMs ?: 0) > System.currentTimeMillis() - LIVE_WINDOW_MS
    val px = with(LocalDensity.current) { FriendCardWidth.roundToPx() }
    val cover by produceState<ImageBitmap?>(null, track?.coverUrl) {
        value = track?.coverUrl?.let { url -> loadImage(url, px)?.asImageBitmap() }
    }
    Surface(
        color = if (live) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (live) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.width(FriendCardWidth).clip(MaterialTheme.shapes.large).clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(
                    friend.user.name,
                    size = 32.dp,
                    picture = rememberProfileImage(friend.user.id, ProfileImage.Avatar, friend.avatarVersion),
                )
                Spacer(Modifier.width(8.dp))
                Text(friend.user.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.medium)) {
                val image = cover
                if (image != null) {
                    Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(BoomIcons.MusicNote, contentDescription = null, modifier = Modifier.size(40.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            val caption = when {
                !friend.sharing -> stringResource(R.string.feature_social_not_sharing_short)
                live -> stringResource(R.string.feature_social_now)
                track != null -> stringResource(
                    R.string.feature_social_last_heard_at,
                    relativeTime(friend.recent.firstOrNull()?.atMs ?: friend.updatedMs ?: System.currentTimeMillis()),
                )
                else -> stringResource(R.string.feature_social_not_playing)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (live) {
                    LiveDot()
                    Spacer(Modifier.width(6.dp))
                }
                Text(caption, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                track?.title.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                track?.artist.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (live) {
                Spacer(Modifier.height(6.dp))
                // Seis emojis não cabem na largura do cartão: a fila desliza.
                ReactionRow(onReact, Modifier.horizontalScroll(rememberScrollState()))
            }
        }
    }
}

/** Largura de um cartão de amigo na fila. */
private val FriendCardWidth = 200.dp

/**
 * Reações com emoji ao que o amigo está ouvindo (só ao vivo, nada guardado).
 * O emoji escolhido fica marcado por um instante.
 */
@Composable
internal fun ReactionRow(onReact: suspend (String) -> Boolean, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var sent by remember { mutableStateOf<String?>(null) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(sent, failed) {
        if (sent != null || failed) {
            delay(1_500)
            sent = null
            failed = false
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        REACTION_EMOJIS.forEach { emoji ->
            val chosen = sent == emoji
            Surface(
                color = if (chosen) MaterialTheme.colorScheme.primary else Color.Transparent,
                contentColor = if (chosen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                shape = CircleShape,
                modifier = Modifier.size(40.dp).clip(CircleShape).clickable {
                    scope.launch { if (onReact(emoji)) sent = emoji else failed = true }
                },
            ) {
                Box(contentAlignment = Alignment.Center) { Text(emoji, style = MaterialTheme.typography.titleMedium) }
            }
        }
        if (failed) {
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.feature_social_react_offline), style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** Uma música que um amigo recomendou: tocar (se você tem) ou abrir o link. */
@Composable
private fun RecommendationRow(
    rec: Recommendation,
    viewModel: SocialHomeViewModel,
    onOpenProfile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val px = with(LocalDensity.current) { 56.dp.roundToPx() }
    val cover by produceState<ImageBitmap?>(null, rec.card.coverUrl) {
        value = rec.card.coverUrl?.let { url -> viewModel.image(url, px)?.asImageBitmap() }
    }
    val owned = viewModel.localMatch(rec.card) != null
    ListItem(
        modifier = modifier.clickable {
            if (!viewModel.play(rec.card)) rec.card.link?.let(context::openUrl)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            Box(Modifier.size(56.dp).clip(MaterialTheme.shapes.medium)) {
                val image = cover
                if (image != null) {
                    Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxSize()) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(BoomIcons.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }
        },
        headlineContent = { Text(rec.card.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                listOfNotNull(
                    rec.card.artist,
                    rec.from?.let { stringResource(R.string.feature_social_recommended_by, it.name) },
                    relativeTime(rec.atMs),
                ).joinToString(" · "),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (owned) Icon(BoomIcons.Owned, stringResource(R.string.feature_social_you_have_it), tint = MaterialTheme.colorScheme.primary)
                BoomMoreMenu(
                    actions = listOfNotNull(
                        rec.from?.let { from ->
                            BoomMenuAction(stringResource(R.string.feature_social_see_profile)) { onOpenProfile(from.id) }
                        },
                        rec.card.link?.let { link ->
                            BoomMenuAction(stringResource(R.string.feature_social_open_link)) { context.openUrl(link) }
                        },
                        BoomMenuAction(stringResource(R.string.feature_social_remove)) { viewModel.removeRecommendation(rec.id) },
                    ),
                )
            }
        },
    )
}

/** "Adicionar amigo": procura pelo @ e abre o perfil (onde fica o botão de pedir amizade). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddFriendSheet(
    viewModel: SocialHomeViewModel,
    onOpenProfile: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Int?>(null) }
    var found by remember { mutableStateOf<FriendProfile?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().imePadding().padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Text(stringResource(R.string.feature_social_add_friend), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.feature_social_add_friend_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = input,
                onValueChange = {
                    input = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' || c == '.' }.take(20)
                    found = null
                    error = null
                },
                label = { Text(stringResource(R.string.feature_social_handle)) },
                prefix = { Text("@") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            error?.let {
                Spacer(Modifier.height(4.dp))
                Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            found?.let { profile ->
                ListItem(
                    modifier = Modifier.clickable { onOpenProfile(profile.user.id) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = {
                        Avatar(
                            profile.user.name,
                            picture = rememberProfileImage(profile.user.id, ProfileImage.Avatar, profile.avatarVersion),
                        )
                    },
                    headlineContent = { Text(profile.user.name) },
                    supportingContent = {
                        Text(
                            when (profile.relation) {
                                Relation.Friend -> stringResource(R.string.feature_social_already_friends)
                                Relation.Sent -> stringResource(R.string.feature_social_request_sent)
                                Relation.Received -> stringResource(R.string.feature_social_request_received)
                                else -> "@" + profile.user.handle
                            },
                        )
                    },
                    trailingContent = {
                        if (profile.relation == Relation.None || profile.relation == Relation.Received) {
                            FilledTonalButton(onClick = {
                                viewModel.addFriend(profile.user.id)
                                found = profile.copy(relation = if (profile.relation == Relation.Received) Relation.Friend else Relation.Sent)
                            }) {
                                Text(
                                    stringResource(
                                        if (profile.relation == Relation.Received) R.string.feature_social_accept else R.string.feature_social_add,
                                    ),
                                )
                            }
                        }
                    },
                )
            }
            Spacer(Modifier.height(12.dp))
            Row {
                Spacer(Modifier.weight(1f))
                Button(
                    enabled = !working && isValidHandle(input),
                    onClick = {
                        working = true
                        error = null
                        scope.launch {
                            when (val result = viewModel.findUser(input)) {
                                is SocialResult.Ok -> found = result.value
                                SocialResult.Offline -> error = R.string.feature_social_offline
                                else -> error = R.string.feature_social_not_found
                            }
                            working = false
                        }
                    },
                ) { Text(stringResource(R.string.feature_social_search)) }
            }
        }
    }
}

/** Criar o perfil de Amigos (o @), numa folha por cima do seu perfil. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OnboardingSheet(viewModel: SocialHomeViewModel, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val account by viewModel.account.collectAsStateWithLifecycle()
    // Perfil criado: a folha fecha sozinha.
    LaunchedEffect(account) { if (account != null) onDismiss() }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Onboarding(viewModel)
    }
}

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
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
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

/** Um ponto colorido pequeno (usado como "ao vivo"). */
@Composable
internal fun LiveDot() {
    Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape, modifier = Modifier.size(8.dp)) {}
}

/** "O social está descansando": a cota diária do plano gratuito acabou; o player segue normal. */
@Composable
internal fun RestingNotice(untilMs: Long) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val time = remember(untilMs) { android.text.format.DateFormat.getTimeFormat(context).format(java.util.Date(untilMs)) }
    androidx.compose.material3.Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.feature_social_resting_title, time), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.feature_social_resting_message), style = MaterialTheme.typography.bodySmall)
        }
    }
}
