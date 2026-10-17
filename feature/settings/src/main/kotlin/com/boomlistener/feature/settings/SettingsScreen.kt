// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.designsystem.component.BoomScreenScaffold

@Composable
internal fun SettingsScreen(
    onOpenWellbeing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoomScreenScaffold(title = stringResource(R.string.feature_settings_title), modifier = modifier) { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                ListItem(
                    modifier = Modifier.clickable(onClick = onOpenWellbeing),
                    leadingContent = { Icon(BoomIcons.Wellbeing, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.feature_settings_wellbeing)) },
                    supportingContent = { Text(stringResource(R.string.feature_settings_wellbeing_summary)) },
                )
            }
        }
    }
}
