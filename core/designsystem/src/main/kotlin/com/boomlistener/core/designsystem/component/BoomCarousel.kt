// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Carrossel do Material 3 (itens grandes, médios e pequenos, que crescem ao
 * rolar). Fica aqui porque o carrossel ainda é API experimental: se a Google
 * mudar, corrige-se só neste arquivo.
 *
 * [item] recebe o índice e o modificador que recorta o item no formato certo
 * enquanto ele muda de tamanho; aplique-o no fundo do item.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoomCarousel(
    itemCount: Int,
    modifier: Modifier = Modifier,
    preferredItemWidth: Dp = 200.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    item: @Composable (index: Int, itemModifier: Modifier) -> Unit,
) {
    val state = rememberCarouselState { itemCount }
    HorizontalMultiBrowseCarousel(
        state = state,
        preferredItemWidth = preferredItemWidth,
        modifier = modifier,
        itemSpacing = 8.dp,
        contentPadding = contentPadding,
    ) { index ->
        item(index, Modifier.maskClip(MaterialTheme.shapes.extraLarge))
    }
}
