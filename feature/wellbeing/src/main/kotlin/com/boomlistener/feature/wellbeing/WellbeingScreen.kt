// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.wellbeing

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.boomlistener.core.designsystem.component.BoomPlaceholder
import com.boomlistener.core.designsystem.component.BoomScreenScaffold

@Composable
internal fun WellbeingScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoomScreenScaffold(
        title = stringResource(R.string.feature_wellbeing_title),
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        BoomPlaceholder(
            title = stringResource(R.string.feature_wellbeing_title),
            message = stringResource(R.string.feature_wellbeing_message),
            modifier = Modifier.padding(padding),
        )
    }
}
