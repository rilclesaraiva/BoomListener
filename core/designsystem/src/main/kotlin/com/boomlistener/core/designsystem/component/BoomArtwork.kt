// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Tamanho padrão da capa nas listas. */
val BoomListArtworkSize: Dp = 52.dp

/**
 * Capa de música ou álbum. Sem imagem, mostra a inicial do [fallbackText]
 * sobre a cor de destaque do tema.
 */
@Composable
fun BoomArtwork(
    image: ImageBitmap?,
    fallbackText: String,
    modifier: Modifier = Modifier,
    size: Dp = BoomListArtworkSize,
) {
    val shaped = modifier.size(size).clip(MaterialTheme.shapes.medium)
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = shaped,
        )
    } else {
        Box(
            modifier = shaped.background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = fallbackText.trim().firstOrNull()?.uppercase() ?: "♪",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}
