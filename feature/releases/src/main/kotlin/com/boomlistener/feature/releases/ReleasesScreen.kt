// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/boomlistener/blob/main/LICENSE

package com.boomlistener.feature.releases

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.boomlistener.core.designsystem.component.BoomPlaceholder

@Composable
internal fun ReleasesScreen(
    modifier: Modifier = Modifier,
) {
    BoomPlaceholder(
        title = stringResource(R.string.feature_releases_title),
        message = stringResource(R.string.feature_releases_message),
        modifier = modifier,
    )
}
