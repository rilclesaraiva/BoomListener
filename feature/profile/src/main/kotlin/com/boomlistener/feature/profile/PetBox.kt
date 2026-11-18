// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.boomlistener.core.designsystem.component.parseProfileColor
import com.boomlistener.data.profile.PetBackgroundKind
import com.boomlistener.data.profile.PetData
import com.boomlistener.data.profile.PetPack
import com.boomlistener.data.profile.PetPose
import com.boomlistener.data.profile.PetRepository
import com.boomlistener.data.profile.PetState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private val PetBoxHeight = 168.dp
private val PetSpriteHeight = 88.dp
private val PetGrabSlop = 12.dp

/** Arquivos que o seletor oferece: o .zip de um shimeji ou uma imagem. */
private val PET_MIME_TYPES = arrayOf(
    "application/zip",
    "application/x-zip-compressed",
    "application/octet-stream",
    "image/png",
    "image/jpeg",
    "image/webp",
)

/**
 * O bichinho do perfil: uma caixa arredondada onde ele passeia, senta, dorme e reage ao toque
 * (e dança quando [playing]). Sem bichinho importado, mostra um convite para escolher o arquivo.
 * Some se a pessoa desligar nos Ajustes.
 */
@Composable
internal fun PetBox(playing: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val repository = remember { PetData.repository(context) }
    val state by repository.state.collectAsStateWithLifecycle()
    if (!state.loaded || !state.enabled) return
    val pack = state.pack
    if (pack == null) PetInvitation(repository, modifier) else PetStage(state, pack, playing, repository, modifier)
}

@Composable
private fun PetInvitation(repository: PetRepository, modifier: Modifier) {
    val scope = rememberCoroutineScope()
    var failed by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch { failed = !repository.importPack(uri.toString()) }
    }
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = { picker.launch(PET_MIME_TYPES) },
        color = scheme.surfaceContainerHighest.copy(alpha = PET_BACKGROUND_ALPHA),
        contentColor = scheme.onSurface,
        shape = MaterialTheme.shapes.extraLarge,
        border = androidx.compose.foundation.BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(R.string.feature_profile_pet_add_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.feature_profile_pet_add_body),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
            if (failed) {
                Text(
                    stringResource(R.string.feature_profile_pet_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

/** Opacidade do fundo padrão da caixa (a mesma do cartão do mês). */
private const val PET_BACKGROUND_ALPHA = 0.4f

@Composable
private fun PetStage(state: PetState, pack: PetPack, playing: Boolean, repository: PetRepository, modifier: Modifier) {
    // Os quadros do bichinho, lidos do arquivo uma vez por pacote.
    val loaded by produceState(PetImages(), pack, state.packVersion) {
        value = withContext(Dispatchers.Default) {
            val bitmaps = HashMap<String, ImageBitmap>()
            val margins = HashMap<String, Triple<Float, Float, Float>>()
            pack.files.forEach { file ->
                val bytes = repository.frame(file) ?: return@forEach
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@forEach
                margins[file] = transparentMargins(bitmap)
                bitmaps[file] = bitmap.asImageBitmap()
            }
            PetImages(bitmaps, margins)
        }
    }
    val bitmaps = loaded.bitmaps
    val stand = pack.frames(PetPose.Stand).firstOrNull()?.let { bitmaps[it.file] }
    val shape = MaterialTheme.shapes.extraLarge
    val boxModifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(PetBoxHeight)
    if (stand == null) {
        Box(boxModifier)
        return
    }
    val description = stringResource(R.string.feature_profile_pet_description)
    BoxWithConstraints(boxModifier) {
        PetBackground(state, repository, shape, Modifier.fillMaxSize())
        // dp na tela por pixel da imagem: todos os quadros usam a mesma escala (o parado dá a altura).
        val dpPerPixel = PetSpriteHeight.value / stand.height
        val spriteWidth = stand.width * dpPerPixel
        val engine = remember(pack) { PetEngine(abilities = pack.poses.keys) }
        var laidOut by remember(engine) { mutableStateOf(false) }
        LaunchedEffect(engine, maxWidth, maxHeight, spriteWidth) {
            engine.layout(maxWidth.value, maxHeight.value, spriteWidth, PetSpriteHeight.value)
            laidOut = true
        }
        val playingNow by rememberUpdatedState(playing)
        var tick by remember { mutableLongStateOf(0L) }
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        LaunchedEffect(engine, laidOut) {
            if (!laidOut) return@LaunchedEffect
            // Só anda com a tela à vista.
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                var last = androidx.compose.runtime.withFrameNanos { it }
                while (true) {
                    val now = androidx.compose.runtime.withFrameNanos { it }
                    engine.update((now - last) / 1_000_000, playingNow)
                    last = now
                    tick = now
                }
            }
        }
        Canvas(
            Modifier
                .fillMaxSize()
                .semantics { contentDescription = description }
                .pointerInput(engine) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val unit = density
                        val slop = PetGrabSlop.toPx()
                        val left = engine.x * unit - slop
                        val top = engine.y * unit - slop
                        val hit = down.position.x in left..(left + spriteWidth * unit + 2 * slop) &&
                            down.position.y in top..(top + PetSpriteHeight.value * unit + 2 * slop)
                        // Fora do bichinho, o toque segue para a página (rolar).
                        if (!hit) return@awaitEachGesture
                        down.consume()
                        engine.grab()
                        var moved = false
                        drag(down.id) { change ->
                            change.consume()
                            if (!moved && (change.position - down.position).getDistance() > viewConfiguration.touchSlop) moved = true
                            if (moved) engine.drag(change.position.x / unit, change.position.y / unit)
                        }
                        engine.release()
                        // Só encostou: um pulinho.
                        if (!moved) engine.poke()
                    }
                },
        ) {
            @Suppress("UNUSED_VARIABLE")
            val redraw = tick
            if (!laidOut) return@Canvas
            val frame = frameAt(pack.frames(engine.mode.pose()), engine.modeTimeMs)
            val image = frame?.let { bitmaps[it.file] } ?: stand
            val scale = dpPerPixel * density
            val width = image.width * scale
            val height = image.height * scale
            var centerX = (engine.x + spriteWidth / 2) * density
            var bottom = (engine.y + PetSpriteHeight.value) * density
            // Na parede e no teto o desenho encosta na borda da caixa pela parte pintada, não pela borda da imagem.
            // Pacotes cujos desenhos olham para a direita só espelham quando ele vai para a esquerda.
            val flipped = engine.facingRight != pack.facesRight
            val (marginLeft, marginTop, marginRight) = frame?.let { loaded.margins[it.file] } ?: Triple(0f, 0f, 0f)
            when (engine.mode) {
                PetMode.WallHold, PetMode.WallClimb -> {
                    // A folga que conta é a do lado do desenho que encosta na parede, já com o espelhamento.
                    val drawnLeft = if (flipped) marginRight else marginLeft
                    val drawnRight = if (flipped) marginLeft else marginRight
                    centerX += if (engine.facingRight) drawnRight * width else -drawnLeft * width
                }
                PetMode.CeilingClimb -> bottom -= marginTop * height
                else -> Unit
            }
            withTransform({
                // Os desenhos olham (em geral) para a esquerda: espelha quando ele vai para o outro lado.
                if (flipped) scale(-1f, 1f, pivot = Offset(centerX, 0f))
            }) {
                drawImage(
                    image,
                    dstOffset = IntOffset((centerX - width / 2).roundToInt(), (bottom - height).roundToInt()),
                    dstSize = IntSize(width.roundToInt(), height.roundToInt()),
                    filterQuality = FilterQuality.Low,
                )
            }
        }
    }
}

/** Os quadros já lidos e, de cada um, a folga transparente à esquerda e em cima (fração da largura e da altura). */
private class PetImages(
    val bitmaps: Map<String, ImageBitmap> = emptyMap(),
    val margins: Map<String, Triple<Float, Float, Float>> = emptyMap(),
)

/** Folga transparente (esquerda, em cima, direita) do desenho, em fração do tamanho da imagem. */
private fun transparentMargins(bitmap: Bitmap): Triple<Float, Float, Float> {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= 0 || height <= 0) return Triple(0f, 0f, 0f)
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    var left = width
    var right = -1
    var top = height
    for (y in 0 until height) {
        for (x in 0 until width) {
            if ((pixels[y * width + x] ushr 24) > ALPHA_VISIBLE) {
                if (x < left) left = x
                if (x > right) right = x
                if (y < top) top = y
            }
        }
    }
    if (right < 0) return Triple(0f, 0f, 0f)
    return Triple(left.toFloat() / width, top.toFloat() / height, (width - 1 - right).toFloat() / width)
}

private const val ALPHA_VISIBLE = 16

/** O que fica atrás do bichinho: o padrão do perfil, uma cor, uma imagem (pode ser GIF) ou nada. */
@Composable
private fun PetBackground(state: PetState, repository: PetRepository, shape: Shape, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    when (state.backgroundKind) {
        PetBackgroundKind.Profile -> Box(
            modifier
                .clip(shape)
                .background(scheme.surfaceContainerHighest.copy(alpha = PET_BACKGROUND_ALPHA))
                .border(1.dp, scheme.outlineVariant.copy(alpha = 0.5f), shape),
        )
        PetBackgroundKind.Color -> parseProfileColor(state.backgroundColor)?.let { color ->
            Box(modifier.clip(shape).background(color))
        }
        PetBackgroundKind.Image -> {
            val picture by produceState<ProfilePicture?>(null, state.backgroundVersion) {
                value = repository.backgroundImage()?.let { bytes ->
                    withContext(Dispatchers.Default) {
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()?.let { still ->
                            ProfilePicture(still, bytes.takeIf(::looksLikeGif))
                        }
                    }
                }
            }
            picture?.let { ProfilePictureView(it, modifier.clip(shape)) }
        }
        PetBackgroundKind.None -> Unit
    }
}
