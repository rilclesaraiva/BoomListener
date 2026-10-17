// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.boomlistener.core.designsystem.component.BoomAccent
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.designsystem.component.BoomScreenScaffold
import com.boomlistener.core.designsystem.component.BoomSettingsItem
import com.boomlistener.core.designsystem.component.BoomSettingsSection

private const val SOURCE_URL = "https://github.com/rilclesaraiva/BoomListener"
private const val LICENSE_URL = "https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE"

@Composable
internal fun AboutScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    BoomScreenScaffold(
        title = stringResource(R.string.feature_settings_about),
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                Text(
                    text = stringResource(R.string.feature_settings_about_text),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            item {
                BoomSettingsSection(title = null) {
                    BoomSettingsItem(
                        icon = BoomIcons.Info,
                        accent = BoomAccent.Primary,
                        title = stringResource(R.string.feature_settings_version),
                        subtitle = appVersionName(context),
                    )
                    BoomSettingsItem(
                        icon = BoomIcons.Code,
                        accent = BoomAccent.Secondary,
                        title = stringResource(R.string.feature_settings_source),
                        subtitle = stringResource(R.string.feature_settings_source_summary),
                        onClick = { context.openUrl(SOURCE_URL) },
                    )
                    BoomSettingsItem(
                        icon = BoomIcons.Article,
                        accent = BoomAccent.Tertiary,
                        title = stringResource(R.string.feature_settings_license),
                        subtitle = stringResource(R.string.feature_settings_license_summary),
                        onClick = { context.openUrl(LICENSE_URL) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun PrivacyScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    BoomScreenScaffold(
        title = stringResource(R.string.feature_settings_privacy),
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                Text(
                    text = stringResource(R.string.feature_settings_privacy_text),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(20.dp),
                )
            }
        }
    }
}

/** Uma biblioteca ou serviço usado pelo app, com sua licença. */
private data class Attribution(val name: String, val detail: String)

private val attributions = listOf(
    Attribution("Jetpack Compose & AndroidX", "Google · Apache License 2.0"),
    Attribution("Material 3 & Material Icons", "Google · Apache License 2.0"),
    Attribution("Media3 (ExoPlayer)", "Google · Apache License 2.0"),
    Attribution("DataStore", "Google · Apache License 2.0"),
    Attribution("Kotlin & kotlinx.coroutines", "JetBrains · Apache License 2.0"),
    Attribution("Palette", "Google · Apache License 2.0"),
    Attribution("Reorderable", "Calvin Liang · Apache License 2.0"),
    Attribution("LRCLIB", "lrclib.net · lyrics database"),
    Attribution("TagLib", "TagLib team · LGPL 2.1 / MPL 1.1"),
    Attribution("TagLib for Android", "Kyant · Apache License 2.0"),
    Attribution("Deezer API", "Deezer · artist information"),
    Attribution("Last.fm API", "Last.fm · biographies (CC BY-SA)"),
)

@Composable
internal fun LicensesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    BoomScreenScaffold(
        title = stringResource(R.string.feature_settings_licenses),
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                BoomSettingsSection(title = null) {
                    attributions.forEach { BoomSettingsItem(icon = null, title = it.name, subtitle = it.detail) }
                }
            }
        }
    }
}

internal fun Context.openUrl(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // Sem navegador instalado: não há o que fazer.
    }
}
