// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.releases

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.boomlistener.core.designsystem.component.BoomPlaceholder
import com.boomlistener.core.designsystem.component.BoomScreenScaffold
import com.boomlistener.core.designsystem.component.BoomTopActions

@Composable
internal fun ReleasesScreen(
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoomScreenScaffold(
        title = stringResource(R.string.feature_releases_title),
        modifier = modifier,
        actions = { BoomTopActions(onOpenHistory = onOpenHistory, onOpenSettings = onOpenSettings) },
    ) { padding ->
        BoomPlaceholder(
            title = stringResource(R.string.feature_releases_title),
            message = stringResource(R.string.feature_releases_message),
            modifier = Modifier.padding(padding),
        )
    }
}
