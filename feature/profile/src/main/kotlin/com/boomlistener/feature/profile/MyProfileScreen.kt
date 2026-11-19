// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.profile

import com.boomlistener.core.designsystem.component.BoomModalSheet
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import com.boomlistener.core.designsystem.component.BoomProfilePalette
import com.boomlistener.core.designsystem.component.parseProfileColor
import com.boomlistener.core.designsystem.component.profileBackground
import com.boomlistener.core.designsystem.component.BoomProfileSurface
import com.boomlistener.core.designsystem.component.clickableWithMenu
import com.boomlistener.core.designsystem.component.BoomPageTitle
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
import com.boomlistener.data.profile.ProfileImage
import com.boomlistener.data.profile.MAX_BIO
import com.boomlistener.data.profile.MyProfile
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
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
import com.boomlistener.core.designsystem.component.LocalBottomOverlay
import com.boomlistener.core.designsystem.component.ScrollToTopOnReselect
import com.boomlistener.core.designsystem.component.artworkTintedBackground
import com.boomlistener.core.designsystem.component.BoomImageCycle
import com.boomlistener.core.designsystem.component.PinImages
import com.boomlistener.core.designsystem.component.PinItem
import com.boomlistener.core.designsystem.component.rememberImageSet
import com.boomlistener.core.designsystem.component.rememberArtworkColor
import com.boomlistener.core.designsystem.component.rememberMemoryImage
import com.boomlistener.data.deezer.splitArtistNames
import com.boomlistener.data.wellbeing.HistoryEntry
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
 * música mais ouvida, os números e destaques do mês (que abrem as Estatísticas),
 * o calendário de escuta, o histórico e quem você segue.
 * Tudo calculado no celular.
 */
@Composable
internal fun MyProfileScreen(
    viewModel: MyProfileViewModel,
    onOpenArtist: (String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenWrapped: (period: String) -> Unit,
    onOpenSettings: () -> Unit,
    songMenu: ProfileSongMenu,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val wrappedCard by viewModel.wrappedCard.collectAsStateWithLifecycle()
    val saveFailed by viewModel.saveFailed.collectAsStateWithLifecycle()
    val insights by viewModel.insights.collectAsStateWithLifecycle()
    val testerMode by com.boomlistener.core.common.DiagnosticLog.enabled.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf(false) }
    var testerHidden by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    ScrollToTopOnReselect(MY_PROFILE_ROUTE, listState)
    state?.let { PinProfileImages(it, viewModel) }

    // Cor sólida: a página inteira na cor escolhida (preto puro fica preto puro),
    // com textos e cartões em tons que combinam com ela.
    BoomProfileSurface(state?.profile?.color, solid = state?.profile?.solidColor == true) {
        BoomScreenScaffold(
            title = stringResource(R.string.feature_profile_my_profile),
            modifier = modifier,
            // O título faz parte da página (primeira linha da lista): fica no topo
            // dela e sobe junto ao rolar, sem barra presa na tela.
            topBar = {},
            // A cor que você escolheu tinge o perfil inteiro, o título junto.
            pageBackground = { profileBackground(state?.profile?.color, solid = state?.profile?.solidColor == true) },
        ) { padding ->
            val current = state
            if (current == null) {
                BoomLoading(Modifier.padding(padding))
                return@BoomScreenScaffold
            }
            // Dia marcado no calendário; tocar em qualquer lugar fora dos quadradinhos desmarca.
            var calendarDay by remember { mutableStateOf<java.time.LocalDate?>(null) }
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = 32.dp + LocalBottomOverlay.current),
                modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { calendarDay = null } },
            ) {
                item(key = "title") {
                    ProfileTitleBar(onEdit = { editing = true }, onOpenSettings = onOpenSettings)
                }
                item(key = "header") {
                    Header(current, viewModel, onEdit = { editing = true })
                    if (saveFailed) {
                        Text(
                            stringResource(R.string.feature_profile_save_failed),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                }
                // Convite do Wrapped (dezembro). Com o registro de diagnóstico ligado (builds de
                // teste) aparece sempre, para abrir o Wrapped a qualquer hora.
                if (wrappedCard || (testerMode && !testerHidden)) {
                    item(key = "wrapped-ready") {
                        WrappedReadyCard(
                            year = current.today.year,
                            onOpen = { onOpenWrapped(com.boomlistener.data.wellbeing.StatsPeriod.ThisYear.name) },
                            onDismiss = {
                                viewModel.dismissWrappedCard()
                                // Nas builds de teste o cartão é forçado: dispensar o esconde até reabrir o app.
                                testerHidden = true
                            },
                        )
                    }
                }
                // O cartão laranja do mês (com os destaques que trocam) abre a página completa de Estatísticas.
                item(key = "month") { MonthCard(current, insights?.highlights.orEmpty(), viewModel, onClick = onOpenStats) }

                insights?.calendar?.takeIf { it.longestStreak > 0 }?.let { calendar ->
                    item(key = "calendar") { CalendarCard(calendar, calendarDay) { calendarDay = it } }
                }

                if (current.recent.isNotEmpty()) {
                    item(key = "history") {
                        SectionTitle(
                            stringResource(R.string.feature_profile_history),
                            null,
                            onMore = onOpenHistory,
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                            items(current.recent, key = { "${it.timestampMs}-${it.uri}" }) { entry ->
                                HistoryCard(entry, current.today, viewModel, songMenu, onClick = { viewModel.playRecent(entry) })
                            }
                        }
                    }
                }

                if (current.following.isNotEmpty()) {
                    item(key = "following") {
                        SectionTitle(stringResource(R.string.feature_profile_following), null)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                            items(current.following, key = { it.deezerId }) { artist ->
                                PersonChip(
                                    name = artist.name,
                                    caption = null,
                                    viewModel = viewModel,
                                    onClick = { onOpenArtist(artist.name) },
                                    onPlay = { viewModel.playArtist(artist.name, shuffled = false) },
                                    onShuffle = { viewModel.playArtist(artist.name, shuffled = true) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val profile = state?.profile
    if (editing && profile != null) {
        EditProfileSheet(profile = profile, viewModel = viewModel, onDismiss = { editing = false })
    }
}

/** Título "Seu perfil" com editar e Ajustes: o topo da página, não uma barra presa na tela. */
@Composable
private fun ProfileTitleBar(onEdit: () -> Unit, onOpenSettings: () -> Unit) {
    BoomPageTitle(stringResource(R.string.feature_profile_my_profile)) {
        IconButton(onClick = onEdit) {
            Icon(BoomIcons.Edit, contentDescription = stringResource(R.string.feature_profile_edit_profile))
        }
        IconButton(onClick = onOpenSettings) {
            Icon(BoomIcons.Settings, contentDescription = stringResource(R.string.feature_profile_settings))
        }
    }
}

/**
 * Cabeçalho: a capa (ou o tom da sua mais ouvida, se ainda não tiver capa),
 * a foto encaixada na borda, nome e descrição. A capa e a foto não são toques:
 * trocar é só pelo lápis ("Editar perfil"); os convites "Deixe do seu jeito" e
 * "Adicionar descrição" também abrem o editar.
 */
@Composable
private fun Header(state: MyProfileUiState, viewModel: MyProfileViewModel, onEdit: () -> Unit) {
    val profile = state.profile
    val banner = rememberProfileImage(ProfileImage.Banner, profile.bannerVersion)
    val photo = rememberProfileImage(ProfileImage.Avatar, profile.avatarVersion)
    val coverUri = state.topSongs.firstOrNull()?.uri
    val cover = rememberLocalArtwork(viewModel, coverUri, 256.dp)
    val seed = rememberArtworkColor(cover)
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Box(contentAlignment = Alignment.BottomStart) {
            if (banner != null) {
                ProfileBanner(
                    banner,
                    Modifier.padding(bottom = 44.dp).clip(MaterialTheme.shapes.extraLarge),
                )
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
                profile.name,
                size = 96.dp,
                picture = photo,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .border(4.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .clip(CircleShape),
            )
        }
        Spacer(Modifier.height(8.dp))
        Column(Modifier.padding(horizontal = 4.dp)) {
            if (profile.name.isNotBlank()) {
                Text(
                    profile.name,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                // Sem nome ainda: um convite para deixar o perfil do seu jeito.
                Text(
                    stringResource(R.string.feature_profile_make_it_yours),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clip(MaterialTheme.shapes.small).clickable(onClick = onEdit),
                )
            }
            Spacer(Modifier.height(8.dp))
            if (profile.bio.isNotBlank()) {
                Text(profile.bio, style = MaterialTheme.typography.bodyLarge)
            } else {
                Text(
                    stringResource(R.string.feature_profile_bio_add),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clip(MaterialTheme.shapes.small).clickable(onClick = onEdit),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

/**
 * Editar o perfil: capa, foto (do seletor de fotos do Android, sem pedir
 * permissão), nome, descrição e cor. Tudo fica só aqui na folha (com prévia)
 * até tocar em Salvar. Fica só no aparelho.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileSheet(profile: MyProfile, viewModel: MyProfileViewModel, onDismiss: () -> Unit) {
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf(profile.name) }
    var bio by remember { mutableStateOf(profile.bio) }
    var bannerChange by remember { mutableStateOf<ImageChange>(ImageChange.Keep) }
    var color by remember { mutableStateOf(profile.color) }
    var solid by remember { mutableStateOf(profile.solidColor) }
    var avatarChange by remember { mutableStateOf<ImageChange>(ImageChange.Keep) }
    val savedBanner = rememberProfileImage(ProfileImage.Banner, profile.bannerVersion)
    val savedPhoto = rememberProfileImage(ProfileImage.Avatar, profile.avatarVersion)
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

    BoomModalSheet(
        onDismissRequest = onDismiss,
        dismissEnabled = !saving,
        // Fecha puxando para baixo, mas só pela faixa de cima (do puxador):
        // rolar o formulário até o topo e continuar puxando fechava sem querer.
        dragOnlyFromTop = true,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.feature_profile_edit_profile),
                style = MaterialTheme.typography.headlineSmall,
            )

            // Capa: tocar troca; "Tirar capa" tira.
            Box(contentAlignment = Alignment.Center) {
                ProfileBanner(
                    banner,
                    Modifier.clip(MaterialTheme.shapes.extraLarge).clickable(enabled = !saving) { pick(ProfileImage.Banner) },
                )
                ImageEditBadge(busy = false, label = stringResource(R.string.feature_profile_banner_change))
            }
            Text(
                stringResource(R.string.feature_profile_gif_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (banner != null) {
                TextButton(onClick = { bannerChange = ImageChange.Remove }, enabled = !saving) {
                    Text(stringResource(R.string.feature_profile_banner_remove))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(
                    name,
                    size = 88.dp,
                    picture = photo,
                    modifier = Modifier.clip(CircleShape).clickable(enabled = !saving) { pick(ProfileImage.Avatar) },
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    TextButton(onClick = { pick(ProfileImage.Avatar) }, enabled = !saving) {
                        Text(stringResource(R.string.feature_profile_photo_change))
                    }
                    if (photo != null) {
                        TextButton(onClick = { avatarChange = ImageChange.Remove }, enabled = !saving) {
                            Text(stringResource(R.string.feature_profile_photo_remove))
                        }
                    }
                }
            }

            ProfileColorPicker(color = color, enabled = !saving, onChange = { color = it })
            // Com cor: degradê (do topo para baixo) ou a cor no perfil inteiro.
            if (color.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !solid,
                        onClick = { solid = false },
                        enabled = !saving,
                        label = { Text(stringResource(R.string.feature_profile_color_gradient)) },
                    )
                    FilterChip(
                        selected = solid,
                        onClick = { solid = true },
                        enabled = !saving,
                        label = { Text(stringResource(R.string.feature_profile_color_solid)) },
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                singleLine = true,
                enabled = !saving,
                label = { Text(stringResource(R.string.feature_profile_name)) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it.take(MAX_BIO) },
                minLines = 3,
                maxLines = 5,
                enabled = !saving,
                label = { Text(stringResource(R.string.feature_profile_bio)) },
                placeholder = { Text(stringResource(R.string.feature_profile_bio_hint)) },
                supportingText = { Text("${bio.length}/$MAX_BIO") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                enabled = !saving,
                onClick = { viewModel.saveProfile(
                        name,
                        bio,
                        bannerChange,
                        avatarChange,
                        color = color,
                        solidColor = solid,
                        onDone = onDismiss,
                    ) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (saving) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.feature_profile_saving))
                } else {
                    Text(stringResource(R.string.feature_profile_save))
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
        Text(stringResource(R.string.feature_profile_color), style = MaterialTheme.typography.titleMedium)
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
            label = { Text(stringResource(R.string.feature_profile_color_hex)) },
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
        if (fill == null) Icon(BoomIcons.Close, contentDescription = stringResource(R.string.feature_profile_color_none), modifier = Modifier.size(18.dp))
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

@Composable
internal fun SectionTitle(title: String, subtitle: String?, onMore: (() -> Unit)? = null) {
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
            Text(stringResource(R.string.feature_profile_see_all), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Barras dos últimos 7 dias; o dia que você mais ouviu fica em destaque. */
@Composable
internal fun WeekCard(state: MyProfileUiState) {
    val week = state.stats.week
    val max = (week.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(1)
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(R.string.feature_profile_daily), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            if (day.date == state.today) stringResource(R.string.feature_profile_today_short)
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
internal fun shortDuration(minutes: Int): String =
    if (minutes >= 60) stringResource(R.string.feature_profile_hours_short, minutes / 60, minutes % 60)
    else stringResource(R.string.feature_profile_minutes, minutes)

/** Manhã, tarde, noite e madrugada: quando você mais ouve. */
@Composable
internal fun DayPartsCard(state: MyProfileUiState) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.feature_profile_peak_hours), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            listOf(
                DayPart.Morning to R.string.feature_profile_morning,
                DayPart.Afternoon to R.string.feature_profile_afternoon,
                DayPart.Night to R.string.feature_profile_night,
                DayPart.Dawn to R.string.feature_profile_dawn,
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

/** Bolinha com nome embaixo (artista). */
@Composable
private fun PersonChip(
    name: String,
    caption: String?,
    viewModel: MyProfileViewModel,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
) {
    val px = with(LocalDensity.current) { 72.dp.roundToPx() }
    val photos = rememberImageSet(remember(name, px) { artistAttempts(name, px, viewModel) })
    val photo = photos.firstOrNull()
    // Segurar abre o menu do artista.
    val menuOpen = remember { mutableStateOf(false) }
    ProfileArtistMenuSheet(menuOpen, name, photo, onPlay = onPlay, onShuffle = onShuffle, onOpenArtist = onClick)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(88.dp).clip(MaterialTheme.shapes.medium).clickableWithMenu(menuOpen, onClick = onClick).padding(top = 4.dp, bottom = TileBottom),
    ) {
        BoomImageCycle(photos) { shown -> Avatar(name, size = 72.dp, image = shown) }
        Spacer(Modifier.height(6.dp))
        Text(name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        caption?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun HistoryCard(entry: HistoryEntry, today: LocalDate, viewModel: MyProfileViewModel, menu: ProfileSongMenu, onClick: () -> Unit) {
    val cover = rememberLocalArtwork(viewModel, entry.uri, CoverCard)
    // Segurar abre o menu da música.
    val menuOpen = remember { mutableStateOf(false) }
    ProfileSongMenuSheet(menuOpen, entry.uri, entry.title, entry.artist, cover, menu, onPlay = onClick)
    val date = remember(entry.timestampMs) { Instant.ofEpochMilli(entry.timestampMs).atZone(ZoneId.systemDefault()).toLocalDate() }
    val label = when (date) {
        today -> stringResource(R.string.feature_profile_today)
        today.minusDays(1) -> stringResource(R.string.feature_profile_yesterday)
        else -> date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
    }
    Column(Modifier.width(CoverCard).clip(MaterialTheme.shapes.medium).clickableWithMenu(menuOpen, onClick = onClick).padding(bottom = TileBottom)) {
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
internal fun rememberLocalArtwork(viewModel: MyProfileViewModel, uri: String?, size: Dp): ImageBitmap? {
    val px = with(LocalDensity.current) { size.roundToPx() }
    val version by viewModel.libraryVersion.collectAsStateWithLifecycle()
    return rememberMemoryImage(uri?.let { coverKey(it, px, version) }) { uri?.let { viewModel.artwork(it, px) } }
}

/** Chave da capa na memória de imagens (a mesma do Início e da Biblioteca: é a mesma imagem). */
private fun coverKey(uri: String, px: Int, version: Int) = "cover:$uri@$px#$version"

/** Chave da foto do artista no perfil (própria: aqui ela cai na capa de uma música dele quando não há foto). */
private fun artistKey(name: String, px: Int) = "profileArtist:$name@$px"

/** Chave da foto do Deezer, sem cair na capa: o que o rodízio de uma tag com vários artistas usa. */
private fun artistPhotoOnlyKey(name: String, px: Int) = "profileArtistPhoto:$name@$px"

/**
 * As imagens de [name] por tentativas. Um artista só: a foto, ou a capa do último
 * lançamento dele. Vários ("Delinquent, Kcat"): a foto do nome inteiro (bandas como
 * "Earth, Wind & Fire"), senão a de cada artista (foto, ou a capa do último lançamento).
 */
internal fun artistAttempts(name: String, px: Int, viewModel: MyProfileViewModel): List<List<PinItem>> {
    fun withCover(n: String) = PinItem(artistKey(n, px)) { viewModel.artistImage(n, px) }
    val parts = splitArtistNames(name).takeIf { it.size > 1 } ?: return listOf(listOf(withCover(name)))
    return listOf(
        listOf(PinItem(artistPhotoOnlyKey(name, px)) { viewModel.artistImage(name, px, coverFallback = false) }),
        parts.map { withCover(it) },
    )
}

/**
 * As imagens do perfil ficam prontas na memória (mais ouvidas, histórico e artistas):
 * deslizar as fileiras ou subir ao topo não deixa nenhuma piscar a inicial.
 */
@Composable
private fun PinProfileImages(state: MyProfileUiState, viewModel: MyProfileViewModel) {
    val density = LocalDensity.current
    val coverPx = with(density) { CoverCard.roundToPx() }
    val artistPx = with(density) { 72.dp.roundToPx() }
    val version by viewModel.libraryVersion.collectAsStateWithLifecycle()
    val items = buildList {
        state.following.forEach { artist ->
            addAll(artistAttempts(artist.name, artistPx, viewModel).flatten())
        }
        (state.topSongs.map { it.uri } + state.recent.map { it.uri }).distinct().forEach { uri ->
            add(PinItem(coverKey(uri, coverPx, version)) { viewModel.artwork(uri, coverPx) })
        }
    }
    PinImages("profileImages", items)
}
