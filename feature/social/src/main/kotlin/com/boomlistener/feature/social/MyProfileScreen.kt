// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.social

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.Color
import com.boomlistener.core.designsystem.component.BoomProfilePalette
import com.boomlistener.core.designsystem.component.parseProfileColor
import com.boomlistener.core.designsystem.component.profileBackground
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.boomlistener.data.social.ProfileImage
import com.boomlistener.data.social.SocialAccount
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.core.designsystem.component.BoomArtwork
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.designsystem.component.BoomLoading
import com.boomlistener.core.designsystem.component.BoomScreenScaffold
import com.boomlistener.core.designsystem.component.artworkTintedBackground
import com.boomlistener.core.designsystem.component.rememberArtworkColor
import com.boomlistener.data.wellbeing.HistoryEntry
import com.boomlistener.data.wellbeing.SongStat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val CoverCard = 136.dp

// Folga para o canto arredondado do toque não cortar as letras.
private val TileBottom = 8.dp
private val TileTextInset = 4.dp

/**
 * O seu perfil, no estilo de um "cartão de ouvinte": cabeçalho no tom da sua
 * música mais ouvida, os números do mês, o que mais tocou, quando você ouve,
 * seus artistas e o histórico. Tudo calculado no celular.
 */
@Composable
internal fun MyProfileScreen(
    viewModel: MyProfileViewModel,
    onBack: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenFriend: (userId: String) -> Unit,
    onOpenHistory: () -> Unit,
    onCreateProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val saveFailed by viewModel.saveFailed.collectAsStateWithLifecycle()
    val friends by viewModel.friends.collectAsStateWithLifecycle()
    var renaming by remember { mutableStateOf(false) }
    var editingNetworks by remember { mutableStateOf(false) }

    BoomScreenScaffold(
        title = stringResource(R.string.feature_social_my_profile),
        onBack = onBack,
        modifier = modifier,
        actions = {
            if (state?.account != null) {
                IconButton(onClick = { renaming = true }) {
                    Icon(BoomIcons.Edit, contentDescription = stringResource(R.string.feature_social_edit_profile))
                }
            }
        },
    ) { padding ->
        val current = state
        if (current == null) {
            BoomLoading(Modifier.padding(padding))
            return@BoomScreenScaffold
        }
        LazyColumn(
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 32.dp),
            // A cor que a pessoa escolheu tinge o perfil.
            modifier = Modifier.fillMaxSize().profileBackground(current.account?.color),
        ) {
            item(key = "header") {
                Header(
                    current,
                    viewModel,
                    onCreateProfile,
                    onToggleSharing = viewModel::setSharing,
                    onEdit = { renaming = true },
                )
                if (saveFailed) {
                    Text(
                        stringResource(R.string.feature_social_offline),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }
            current.account?.let { account ->
                item(key = "networks") {
                    // Suas redes (como os outros veem; o número, só os amigos).
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        NetworkButtons(
                            links = account.links,
                            numbers = if (account.shareNumbers) account.numbers else emptyMap(),
                        )
                        TextButton(onClick = { editingNetworks = true }) {
                            Text(
                                stringResource(
                                    if (account.links.isEmpty() && account.numbers.isEmpty()) {
                                        R.string.feature_social_networks_add
                                    } else {
                                        R.string.feature_social_networks_edit
                                    },
                                ),
                            )
                        }
                    }
                }
            }
            item(key = "month") { MonthCard(current) }

            if (current.topSongs.isNotEmpty()) {
                item(key = "top") {
                    SectionTitle(stringResource(R.string.feature_social_most_played), stringResource(R.string.feature_social_most_played_sub))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                        itemsIndexed(current.topSongs, key = { _, song -> song.uri }) { index, song ->
                            TopSongCard(index + 1, song, viewModel, onClick = { viewModel.playTop(index) })
                        }
                    }
                }
            }

            item(key = "time") {
                SectionTitle(stringResource(R.string.feature_social_listening_time), stringResource(R.string.feature_social_last_7_days))
                // Um embaixo do outro, na largura da tela: nada fica cortado na borda.
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    WeekCard(current)
                    DayPartsCard(current)
                }
            }

            if (current.stats.topArtists.isNotEmpty()) {
                item(key = "artists") {
                    SectionTitle(stringResource(R.string.feature_social_top_artists), stringResource(R.string.feature_social_last_30_days))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                        items(current.stats.topArtists, key = { it.name }) { artist ->
                            PersonChip(
                                name = artist.name,
                                caption = pluralStringResource(R.plurals.feature_social_plays, artist.plays, artist.plays),
                                viewModel = viewModel,
                                onClick = { onOpenArtist(artist.name) },
                            )
                        }
                    }
                }
            }

            if (current.recent.isNotEmpty()) {
                item(key = "history") {
                    SectionTitle(
                        stringResource(R.string.feature_social_history),
                        null,
                        onMore = onOpenHistory,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                        items(current.recent, key = { "${it.timestampMs}-${it.uri}" }) { entry ->
                            HistoryCard(entry, current.today, viewModel, onClick = { viewModel.playRecent(entry) })
                        }
                    }
                }
            }

            // Seus amigos (amizade confirmada). Os artistas seguidos já
            // aparecem na aba Lançamentos.
            item(key = "friends") {
                SectionTitle(stringResource(R.string.feature_social_friends_count, friends.size), null)
                if (friends.isEmpty()) {
                    Text(
                        stringResource(R.string.feature_social_friends_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                        items(friends, key = { it.userId }) { friend ->
                            FriendChipView(friend, viewModel, onClick = { onOpenFriend(friend.userId) })
                        }
                    }
                }
            }
        }
    }

    val account = state?.account
    if (renaming && account != null) {
        EditProfileSheet(account = account, viewModel = viewModel, onDismiss = { renaming = false })
    }
    if (editingNetworks && account != null) {
        NetworksSheet(account = account, viewModel = viewModel, onDismiss = { editingNetworks = false })
    }
}

/**
 * Cabeçalho: a capa (ou o tom da sua mais ouvida, se ainda não tiver capa),
 * a foto encaixada na borda, nome, @, descrição e o compartilhamento.
 */
@Composable
private fun Header(
    state: MyProfileUiState,
    viewModel: MyProfileViewModel,
    onCreateProfile: () -> Unit,
    onToggleSharing: (Boolean) -> Unit,
    onEdit: () -> Unit,
) {
    val account = state.account
    val banner = rememberProfileImage(account?.id, ProfileImage.Banner, account?.bannerVersion ?: 0)
    val photo = rememberProfileImage(account?.id, ProfileImage.Avatar, account?.avatarVersion ?: 0)
    val coverUri = state.topSongs.firstOrNull()?.uri
    val cover = rememberLocalArtwork(viewModel, coverUri, 256.dp)
    val seed = rememberArtworkColor(cover)
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Box(contentAlignment = Alignment.BottomStart) {
            if (banner != null) {
                ProfileBanner(banner, Modifier.padding(bottom = 44.dp).clip(MaterialTheme.shapes.extraLarge))
            } else {
                // Sem capa: o tom da sua música mais ouvida.
                Box(
                    Modifier
                        .padding(bottom = 44.dp)
                        .fillMaxWidth()
                        .aspectRatio(BANNER_RATIO)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .artworkTintedBackground(seed),
                )
            }
            Avatar(
                account?.name ?: "?",
                size = 96.dp,
                picture = photo,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .border(4.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .clip(CircleShape)
                    .then(if (account != null) Modifier.clickable(onClick = onEdit) else Modifier),
            )
        }
        Spacer(Modifier.height(8.dp))
        Column(Modifier.padding(horizontal = 4.dp)) {
            Text(
                account?.name ?: stringResource(R.string.feature_social_you),
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                account?.let { "@" + it.handle } ?: stringResource(R.string.feature_social_no_profile_yet),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (account != null) {
                Spacer(Modifier.height(8.dp))
                if (account.bio.isNotBlank()) {
                    Text(account.bio, style = MaterialTheme.typography.bodyLarge)
                } else {
                    // Sem descrição: um convite para escrever uma.
                    Text(
                        stringResource(R.string.feature_social_bio_add),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clip(MaterialTheme.shapes.small).clickable(onClick = onEdit),
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (account == null) {
            FilledTonalButton(onClick = onCreateProfile) { Text(stringResource(R.string.feature_social_create_profile)) }
        } else {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        stringResource(R.string.feature_social_share_title),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = account.sharing, onCheckedChange = onToggleSharing)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/**
 * Editar o perfil: capa, foto (do seletor de fotos do Android, sem pedir
 * permissão), nome e descrição. Tudo fica só aqui na folha (com prévia) até
 * tocar em Salvar; aí vai para o servidor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileSheet(account: SocialAccount, viewModel: MyProfileViewModel, onDismiss: () -> Unit) {
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf(account.name) }
    var bio by remember { mutableStateOf(account.bio) }
    var bannerChange by remember { mutableStateOf<ImageChange>(ImageChange.Keep) }
    var color by remember { mutableStateOf(account.color) }
    var avatarChange by remember { mutableStateOf<ImageChange>(ImageChange.Keep) }
    val savedBanner = rememberProfileImage(account.id, ProfileImage.Banner, account.bannerVersion)
    val savedPhoto = rememberProfileImage(account.id, ProfileImage.Avatar, account.avatarVersion)
    val pickedBanner = rememberPickedImage((bannerChange as? ImageChange.Set)?.uri)
    val pickedPhoto = rememberPickedImage((avatarChange as? ImageChange.Set)?.uri)
    // Prévia: a escolhida agora, ou nenhuma (se pediu para tirar), ou a que já estava.
    val banner = when (bannerChange) {
        is ImageChange.Set -> pickedBanner
        ImageChange.Remove -> null
        ImageChange.Keep -> savedBanner
    }
    val photo = when (avatarChange) {
        is ImageChange.Set -> pickedPhoto
        ImageChange.Remove -> null
        ImageChange.Keep -> savedPhoto
    }
    var picking by remember { mutableStateOf(ProfileImage.Avatar) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            when (picking) {
                ProfileImage.Banner -> bannerChange = ImageChange.Set(uri.toString())
                ProfileImage.Avatar -> avatarChange = ImageChange.Set(uri.toString())
            }
        }
    }
    fun pick(kind: ProfileImage) {
        picking = kind
        // Foto, GIF ou vídeo curto (vira GIF). O seletor do Android não pede permissão.
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
    }

    ModalBottomSheet(
        onDismissRequest = { if (!saving) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.feature_social_edit_profile), style = MaterialTheme.typography.headlineSmall)

            // Capa: tocar troca; "Tirar capa" tira.
            Box(contentAlignment = Alignment.Center) {
                ProfileBanner(
                    banner,
                    Modifier.clip(MaterialTheme.shapes.extraLarge).clickable(enabled = !saving) { pick(ProfileImage.Banner) },
                )
                ImageEditBadge(busy = false, label = stringResource(R.string.feature_social_banner_change))
            }
            Text(
                stringResource(R.string.feature_social_gif_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (banner != null) {
                TextButton(onClick = { bannerChange = ImageChange.Remove }, enabled = !saving) {
                    Text(stringResource(R.string.feature_social_banner_remove))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(
                    account.name,
                    size = 88.dp,
                    picture = photo,
                    modifier = Modifier.clip(CircleShape).clickable(enabled = !saving) { pick(ProfileImage.Avatar) },
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    TextButton(onClick = { pick(ProfileImage.Avatar) }, enabled = !saving) {
                        Text(stringResource(R.string.feature_social_photo_change))
                    }
                    if (photo != null) {
                        TextButton(onClick = { avatarChange = ImageChange.Remove }, enabled = !saving) {
                            Text(stringResource(R.string.feature_social_photo_remove))
                        }
                    }
                }
            }

            ProfileColorPicker(color = color, enabled = !saving, onChange = { color = it })

            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                singleLine = true,
                enabled = !saving,
                label = { Text(stringResource(R.string.feature_social_name)) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it.take(MAX_BIO) },
                minLines = 3,
                maxLines = 5,
                enabled = !saving,
                label = { Text(stringResource(R.string.feature_social_bio)) },
                placeholder = { Text(stringResource(R.string.feature_social_bio_hint)) },
                supportingText = { Text("${bio.length}/$MAX_BIO") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                enabled = name.isNotBlank() && !saving,
                onClick = { viewModel.saveProfile(name, bio, bannerChange, avatarChange, color = color, onDone = onDismiss) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (saving) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.feature_social_saving))
                } else {
                    Text(stringResource(R.string.feature_social_save))
                }
            }
        }
    }
}

/**
 * Cor do perfil: bolinhas com as cores prontas, "sem cor" e um campo para
 * digitar o hexadecimal (#RRGGBB).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileColorPicker(color: String, enabled: Boolean, onChange: (String) -> Unit) {
    var hex by remember { mutableStateOf(color.removePrefix("#")) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.feature_social_color), style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Sem cor.
            ColorDot(selected = color.isEmpty(), fill = null, enabled = enabled) {
                hex = ""
                onChange("")
            }
            BoomProfilePalette.forEach { option ->
                ColorDot(selected = color.equals(option, ignoreCase = true), fill = parseProfileColor(option), enabled = enabled) {
                    hex = option.removePrefix("#")
                    onChange(option)
                }
            }
        }
        OutlinedTextField(
            value = hex,
            onValueChange = { typed ->
                hex = typed.removePrefix("#").filter { it.isLetterOrDigit() }.take(6).uppercase()
                if (hex.length == 6 && parseProfileColor(hex) != null) onChange("#$hex")
            },
            enabled = enabled,
            singleLine = true,
            prefix = { Text("#") },
            label = { Text(stringResource(R.string.feature_social_color_hex)) },
            isError = hex.isNotEmpty() && parseProfileColor(hex) == null,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ColorDot(selected: Boolean, fill: Color?, enabled: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(fill ?: MaterialTheme.colorScheme.surfaceContainerHighest)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape,
            )
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        if (fill == null) Icon(BoomIcons.Close, contentDescription = stringResource(R.string.feature_social_color_none), modifier = Modifier.size(18.dp))
    }
}

/** Selo no meio da capa: "Trocar capa", ou o carregando enquanto envia. */
@Composable
private fun ImageEditBadge(busy: Boolean, label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = CircleShape,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            if (busy) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(BoomIcons.Image, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** "Este mês": músicas, horas e artistas. */
@Composable
private fun MonthCard(state: MyProfileUiState) {
    val month = state.today.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
        .replaceFirstChar { it.titlecase(Locale.getDefault()) }
    val hours = state.stats.monthMinutes / 60
    val minutes = state.stats.monthMinutes % 60
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(BoomIcons.History, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.feature_social_this_month).uppercase(), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.weight(1f))
                Text(month, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigNumber(state.stats.monthPlays.toString(), pluralStringResource(R.plurals.feature_social_songs, state.stats.monthPlays), Modifier.weight(1f))
                BigNumber(
                    if (hours > 0) stringResource(R.string.feature_social_hours_minutes, hours, minutes) else stringResource(R.string.feature_social_minutes, minutes),
                    stringResource(R.string.feature_social_listened),
                    Modifier.weight(1.4f),
                )
                BigNumber(state.stats.monthArtists.toString(), pluralStringResource(R.plurals.feature_social_artists, state.stats.monthArtists), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BigNumber(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String?, onMore: (() -> Unit)? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onMore != null) Modifier.clickable(onClick = onMore) else Modifier)
            .padding(start = 20.dp, end = 12.dp, top = 24.dp, bottom = 10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (onMore != null) {
            Text(stringResource(R.string.feature_social_see_all), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun TopSongCard(rank: Int, song: SongStat, viewModel: MyProfileViewModel, onClick: () -> Unit) {
    val cover = rememberLocalArtwork(viewModel, song.uri, CoverCard)
    Column(Modifier.width(CoverCard).clip(MaterialTheme.shapes.medium).clickable(onClick = onClick).padding(bottom = TileBottom)) {
        Box {
            BoomArtwork(image = cover, fallbackText = song.title, size = CoverCard)
            // Posição no ranking, num selo no canto.
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.padding(8.dp).size(28.dp),
            ) {
                Box(contentAlignment = Alignment.Center) { Text(rank.toString(), style = MaterialTheme.typography.labelLarge) }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(song.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = TileTextInset))
        Text(
            pluralStringResource(R.plurals.feature_social_plays, song.plays, song.plays),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = TileTextInset),
        )
    }
}

/** Barras dos últimos 7 dias; o dia que você mais ouviu fica em destaque. */
@Composable
private fun WeekCard(state: MyProfileUiState) {
    val week = state.stats.week
    val max = (week.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(1)
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(R.string.feature_social_daily), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.height(140.dp)) {
                week.forEach { day ->
                    val isMax = day.minutes == max && day.minutes > 0
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(
                            if (day.minutes > 0) shortDuration(day.minutes) else "",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                        )
                        Spacer(Modifier.height(4.dp))
                        val fraction = day.minutes.toFloat() / max
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(4.dp + (80 * fraction).dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(
                                    if (isMax) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                ),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (day.date == state.today) stringResource(R.string.feature_social_today_short)
                            else day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun shortDuration(minutes: Int): String =
    if (minutes >= 60) stringResource(R.string.feature_social_hours_short, minutes / 60, minutes % 60)
    else stringResource(R.string.feature_social_minutes, minutes)

/** Manhã, tarde, noite e madrugada: quando você mais ouve. */
@Composable
private fun DayPartsCard(state: MyProfileUiState) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.feature_social_peak_hours), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            listOf(
                DayPart.Morning to R.string.feature_social_morning,
                DayPart.Afternoon to R.string.feature_social_afternoon,
                DayPart.Night to R.string.feature_social_night,
                DayPart.Dawn to R.string.feature_social_dawn,
            ).forEach { (part, label) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(label), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(96.dp))
                    LinearProgressIndicator(
                        progress = { state.stats.dayParts[part] ?: 0f },
                        modifier = Modifier.weight(1f),
                        drawStopIndicator = {},
                    )
                }
            }
        }
    }
}

/** Um amigo: foto (ou inicial) e nome. */
@Composable
private fun FriendChipView(friend: FriendChip, viewModel: MyProfileViewModel, onClick: () -> Unit) {
    val version by produceState(initialValue = 0L, friend.userId) { value = viewModel.friendAvatarVersion(friend.userId) }
    val photo = rememberProfileImage(friend.userId, ProfileImage.Avatar, version)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(88.dp).clip(MaterialTheme.shapes.medium).clickable(onClick = onClick).padding(top = 4.dp, bottom = TileBottom),
    ) {
        Avatar(friend.name, size = 72.dp, picture = photo)
        Spacer(Modifier.height(6.dp))
        Text(friend.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

/** Bolinha com nome embaixo (artista). */
@Composable
private fun PersonChip(name: String, caption: String?, viewModel: MyProfileViewModel, onClick: () -> Unit) {
    val px = with(LocalDensity.current) { 72.dp.roundToPx() }
    val photo by produceState<ImageBitmap?>(initialValue = null, name) {
        value = viewModel.artistImage(name, px)?.asImageBitmap()
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(88.dp).clip(MaterialTheme.shapes.medium).clickable(onClick = onClick).padding(top = 4.dp, bottom = TileBottom),
    ) {
        Avatar(name, size = 72.dp, image = photo)
        Spacer(Modifier.height(6.dp))
        Text(name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        caption?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun HistoryCard(entry: HistoryEntry, today: LocalDate, viewModel: MyProfileViewModel, onClick: () -> Unit) {
    val cover = rememberLocalArtwork(viewModel, entry.uri, CoverCard)
    val date = remember(entry.timestampMs) { Instant.ofEpochMilli(entry.timestampMs).atZone(ZoneId.systemDefault()).toLocalDate() }
    val label = when (date) {
        today -> stringResource(R.string.feature_social_today)
        today.minusDays(1) -> stringResource(R.string.feature_social_yesterday)
        else -> date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
    }
    Column(Modifier.width(CoverCard).clip(MaterialTheme.shapes.medium).clickable(onClick = onClick).padding(bottom = TileBottom)) {
        Box(contentAlignment = Alignment.BottomStart) {
            BoomArtwork(image = cover, fallbackText = entry.title, size = CoverCard)
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape,
                modifier = Modifier.padding(8.dp),
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(entry.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = TileTextInset))
        Text(
            entry.artist.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = TileTextInset),
        )
    }
}

@Composable
private fun rememberLocalArtwork(viewModel: MyProfileViewModel, uri: String?, size: Dp): ImageBitmap? {
    val px = with(LocalDensity.current) { size.roundToPx() }
    val image by produceState<ImageBitmap?>(initialValue = null, uri, px) {
        value = uri?.let { viewModel.artwork(it, px)?.asImageBitmap() }
    }
    return image
}
