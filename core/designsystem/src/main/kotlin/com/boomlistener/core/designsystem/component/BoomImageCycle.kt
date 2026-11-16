// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.boomlistener.core.designsystem.theme.boomEffectsSpec
import kotlinx.coroutines.delay

/** Quanto cada foto fica na tela antes de a próxima entrar. */
internal const val CycleHoldMs = 2000L

/** Quanto esperar antes de cada nova tentativa quando nenhuma imagem veio. */
private val RetryDelaysMs = longArrayOf(2_000L, 5_000L)

/** O índice que vem depois de [index] numa lista de [size] (volta ao começo no fim). */
internal fun nextCycleIndex(index: Int, size: Int): Int = if (size <= 1) 0 else (index + 1) % size

/**
 * Várias imagens de um mesmo item (as fotos de cada artista de uma música com mais
 * de um): uma sai se esvaindo enquanto a próxima aparece, de tempos em tempos.
 * Com uma imagem só (ou nenhuma), mostra direto, sem camada extra.
 */
@Composable
fun BoomImageCycle(images: List<ImageBitmap>, content: @Composable (ImageBitmap?) -> Unit) {
    if (images.size <= 1) {
        content(images.firstOrNull())
        return
    }
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(images.size) {
        while (true) {
            delay(CycleHoldMs)
            index = nextCycleIndex(index, images.size)
        }
    }
    Crossfade(targetState = images[index.coerceIn(0, images.size - 1)], animationSpec = boomEffectsSpec(), label = "imageCycle") { content(it) }
}

/**
 * As imagens de um item, por tentativas: a primeira lista de [attempts] que render
 * alguma imagem vale (o nome inteiro do artista primeiro, que acerta bandas como
 * "Earth, Wind & Fire"; senão, cada artista separado). Já na memória, aparece de
 * imediato; o resto vem por [PinItem.load] e fica guardado.
 */
@Composable
fun rememberImageSet(attempts: List<List<PinItem>>): List<ImageBitmap> {
    val ids = attempts.map { attempt -> attempt.map { it.key } }
    val images by produceState(initialValue = fromMemory(attempts), ids) {
        // Sem nenhuma imagem (o Deezer limita pedidos em rajada e responde vazio): tenta de novo, devagar.
        for (retry in 0..RetryDelaysMs.size) {
            for (attempt in attempts) {
                val found = ArrayList<ImageBitmap>()
                for (item in attempt) {
                    val image = BoomImageMemory.get(item.key) ?: item.load()?.asImageBitmap()?.also { BoomImageMemory.put(item.key, it) }
                    if (image != null) {
                        found += image
                        value = found.toList()
                    }
                }
                if (found.isNotEmpty()) return@produceState
            }
            delay(RetryDelaysMs.getOrNull(retry) ?: return@produceState)
        }
    }
    return images
}

private fun fromMemory(attempts: List<List<PinItem>>): List<ImageBitmap> {
    for (attempt in attempts) {
        val found = attempt.mapNotNull { BoomImageMemory.get(it.key) }
        if (found.isNotEmpty()) return found
    }
    return emptyList()
}
