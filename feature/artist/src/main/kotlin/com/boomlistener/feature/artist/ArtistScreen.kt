// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.artist

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.boomlistener.core.designsystem.component.BoomPlaceholder
import com.boomlistener.core.designsystem.component.BoomScreenScaffold

@Composable
internal fun ArtistScreen(
    artistName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoomScreenScaffold(
        title = artistName.ifBlank { stringResource(R.string.feature_artist_title) },
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        BoomPlaceholder(
            title = artistName.ifBlank { stringResource(R.string.feature_artist_title) },
            message = stringResource(R.string.feature_artist_message),
            modifier = Modifier.padding(padding),
        )
    }
}
