// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.profile

import androidx.compose.ui.semantics.contentDescription
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import android.widget.ImageView
import androidx.compose.runtime.remember
import androidx.compose.ui.viewinterop.AndroidView
import java.nio.ByteBuffer
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
import com.boomlistener.data.profile.ProfileImage
import com.boomlistener.data.profile.ProfileData
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.boomlistener.core.designsystem.component.BoomIcons

/** Bolinha com a sua foto, ou a inicial do nome (ou o ícone de pessoa, sem nome). */
@Composable
internal fun Avatar(
    name: String,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
    image: ImageBitmap? = null,
    picture: ProfilePicture? = null,
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = CircleShape,
        modifier = modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center) {
            when {
                picture != null -> ProfilePictureView(picture, Modifier.fillMaxSize().clip(CircleShape))
                image != null -> Image(
                    bitmap = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
                name.isBlank() -> Icon(BoomIcons.Person, contentDescription = null)
                else -> Text(
                    text = name.trim().firstOrNull()?.uppercase() ?: "?",
                    style = if (size >= 64.dp) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

/**
 * Foto ou capa: [still] é a imagem parada
 * (sempre existe) e [gif] os bytes, quando é um GIF animado.
 */
internal class ProfilePicture(val still: ImageBitmap, val gif: ByteArray?)

/** A sua foto ou capa (guardada no aparelho); null se não tiver. Relê quando [version] muda. */
@Composable
internal fun rememberProfileImage(kind: ProfileImage, version: Long): ProfilePicture? {
    val context = LocalContext.current.applicationContext
    val picture by produceState<ProfilePicture?>(initialValue = null, kind, version) {
        value = if (version <= 0) {
            null
        } else {
            ProfileData.repository(context).image(kind)?.let { bytes ->
                withContext(Dispatchers.Default) {
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()?.let { still ->
                        ProfilePicture(still, bytes.takeIf(::looksLikeGif))
                    }
                }
            }
        }
    }
    return picture
}

/** Prévia da imagem escolhida no seletor (antes de salvar). */
@Composable
internal fun rememberPickedImage(uri: String?): ProfilePicture? {
    val context = LocalContext.current.applicationContext
    val picture by produceState<ProfilePicture?>(initialValue = null, uri) {
        value = uri?.let {
            withContext(Dispatchers.IO) {
                runCatching {
                    // Vídeo: a prévia é o primeiro quadro (ao salvar, ele vira GIF).
                    if (context.contentResolver.getType(Uri.parse(it))?.startsWith("video/") == true) {
                        val retriever = MediaMetadataRetriever()
                        return@runCatching try {
                            retriever.setDataSource(context, Uri.parse(it))
                            retriever.getFrameAtTime(0)?.asImageBitmap()?.let { still -> ProfilePicture(still, null) }
                        } finally {
                            retriever.release()
                        }
                    }
                    val bytes = context.contentResolver.openInputStream(Uri.parse(it))?.use { stream -> stream.readBytes() }
                        ?: return@runCatching null
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                    // Prévia leve: reduz fotos grandes da câmera.
                    var sample = 1
                    while (bounds.outWidth / (sample * 2) >= 1200 && bounds.outHeight / (sample * 2) >= 450) sample *= 2
                    val still = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
                        ?.asImageBitmap() ?: return@runCatching null
                    ProfilePicture(still, bytes.takeIf(::looksLikeGif))
                }.getOrNull()
            }
        }
    }
    return picture
}

private fun looksLikeGif(bytes: ByteArray) =
    bytes.size >= 6 && String(bytes, 0, 6, Charsets.US_ASCII).let { it == "GIF87a" || it == "GIF89a" }

/**
 * Mostra a foto/capa: GIF animado roda (Android 9+, pelo ImageDecoder do
 * sistema); nos anteriores, fica o primeiro quadro.
 */
@Composable
internal fun ProfilePictureView(picture: ProfilePicture, modifier: Modifier = Modifier) {
    val gif = picture.gif
    if (gif != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val drawable = remember(gif) {
            runCatching { ImageDecoder.decodeDrawable(ImageDecoder.createSource(ByteBuffer.wrap(gif))) }.getOrNull()
        }
        if (drawable != null) {
            AndroidView(
                factory = { context -> ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP } },
                update = { view ->
                    if (view.drawable !== drawable) view.setImageDrawable(drawable)
                    (drawable as? AnimatedImageDrawable)?.let { animated ->
                        // Repete para sempre, mesmo que o GIF diga para tocar só uma vez.
                        animated.repeatCount = REPEAT_FOREVER
                        if (!animated.isRunning) animated.start()
                    }
                },
                modifier = modifier,
            )
            return
        }
    }
    Image(bitmap = picture.still, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
}

/** Proporção da capa (a mesma do recorte guardado). */
internal const val BANNER_RATIO = 1200f / 450f

/**
 * Capa do perfil: a imagem escolhida, ou um degradê nas cores do tema
 * quando a pessoa ainda não pôs uma.
 */
@Composable
internal fun ProfileBanner(image: ProfilePicture?, modifier: Modifier = Modifier) {
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
        if (image != null) ProfilePictureView(image, Modifier.fillMaxSize())
    }
}

/** "Repetir para sempre" do AnimatedImageDrawable (REPETITION_INFINITE = -1). */
private const val REPEAT_FOREVER = -1
