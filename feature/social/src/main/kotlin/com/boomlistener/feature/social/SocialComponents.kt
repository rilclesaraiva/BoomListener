// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.social

import android.graphics.BitmapFactory
import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.boomlistener.data.social.ProfileImage
import com.boomlistener.data.social.SocialData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.boomlistener.core.designsystem.component.BoomIcons
import kotlinx.coroutines.delay

/** Bolinha com a foto da pessoa, ou a inicial do nome (ou o ícone de grupo). */
@Composable
internal fun Avatar(
    name: String,
    isGroup: Boolean = false,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
    image: ImageBitmap? = null,
) {
    Surface(
        color = if (isGroup) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (isGroup) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
        shape = CircleShape,
        modifier = modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center) {
            when {
                image != null -> Image(
                    bitmap = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
                isGroup -> Icon(BoomIcons.Person, contentDescription = null)
                else -> Text(
                    text = name.trim().firstOrNull()?.uppercase() ?: "?",
                    style = if (size >= 64.dp) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

/** Foto ou capa de alguém (baixada uma vez e guardada no aparelho); null se não tiver. */
@Composable
internal fun rememberProfileImage(userId: String?, kind: ProfileImage, version: Long): ImageBitmap? {
    val context = LocalContext.current.applicationContext
    val image by produceState<ImageBitmap?>(initialValue = null, userId, kind, version) {
        value = if (userId == null || version <= 0) {
            null
        } else {
            SocialData.repository(context).profileImage(userId, kind, version)?.let { bytes ->
                withContext(Dispatchers.Default) {
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                }
            }
        }
    }
    return image
}

/** Proporção da capa (a mesma do recorte enviado ao servidor). */
internal const val BANNER_RATIO = 1200f / 450f

/**
 * Capa do perfil: a imagem escolhida, ou um degradê nas cores do tema
 * quando a pessoa ainda não pôs uma.
 */
@Composable
internal fun ProfileBanner(image: ImageBitmap?, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(BANNER_RATIO)
            .background(
                Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.tertiaryContainer),
                ),
            ),
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** "agora", "há 5 min", "ontem"… no idioma do celular. */
internal fun relativeTime(timestampMs: Long, nowMs: Long = System.currentTimeMillis()): String =
    DateUtils.getRelativeTimeSpanString(timestampMs, nowMs, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE)
        .toString()

/**
 * Repete [block] a cada [intervalMs] só enquanto a tela está visível (o app
 * em segundo plano não fica consultando o servidor).
 */
@Composable
internal fun PollWhileVisible(intervalMs: Long, block: suspend () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                block()
                delay(intervalMs)
            }
        }
    }
}
