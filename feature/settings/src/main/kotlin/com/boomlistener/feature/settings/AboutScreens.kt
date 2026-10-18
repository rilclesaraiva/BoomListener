// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
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

private const val DEVELOPER_URL = "https://github.com/rilclesaraiva"
private const val CHANGELOG_URL = "https://github.com/rilclesaraiva/BoomListener/blob/main/CHANGELOG.md"
private const val ISSUES_URL = "https://github.com/rilclesaraiva/BoomListener/issues"

@Composable
internal fun AboutScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    BoomScreenScaffold(
        title = stringResource(R.string.feature_settings_about_short),
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            item(key = "hero") { AboutHero() }
            item(key = "text") {
                Text(
                    text = stringResource(R.string.feature_settings_about_text),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp),
                )
            }
            item(key = "developer") {
                BoomSettingsSection(title = stringResource(R.string.feature_settings_about_developer)) {
                    BoomSettingsItem(
                        icon = BoomIcons.Person,
                        accent = BoomAccent.Primary,
                        title = "rilclesaraiva",
                        subtitle = stringResource(R.string.feature_settings_about_developer_summary),
                        onClick = { context.openUrl(DEVELOPER_URL) },
                    )
                }
            }
            item(key = "project") {
                BoomSettingsSection(title = stringResource(R.string.feature_settings_about_project)) {
                    BoomSettingsItem(
                        icon = BoomIcons.Code,
                        accent = BoomAccent.Secondary,
                        title = stringResource(R.string.feature_settings_source),
                        subtitle = stringResource(R.string.feature_settings_source_summary),
                        onClick = { context.openUrl(SOURCE_URL) },
                    )
                    BoomSettingsItem(
                        icon = BoomIcons.NewReleases,
                        accent = BoomAccent.Tertiary,
                        title = stringResource(R.string.feature_settings_about_changelog),
                        subtitle = stringResource(R.string.feature_settings_about_changelog_summary),
                        onClick = { context.openUrl(CHANGELOG_URL) },
                    )
                    BoomSettingsItem(
                        icon = BoomIcons.BugReport,
                        accent = BoomAccent.Primary,
                        title = stringResource(R.string.feature_settings_about_issues),
                        subtitle = stringResource(R.string.feature_settings_about_issues_summary),
                        onClick = { context.openUrl(ISSUES_URL) },
                    )
                    BoomSettingsItem(
                        icon = BoomIcons.Article,
                        title = stringResource(R.string.feature_settings_license),
                        subtitle = stringResource(R.string.feature_settings_license_summary),
                        onClick = { context.openUrl(LICENSE_URL) },
                    )
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

/** Ícone do app, o nome e etiquetas com a versão e o tipo de build. */
@Composable
private fun AboutHero() {
    val context = LocalContext.current
    val icon = remember { appIcon(context) }
    val rawVersion = remember { rawVersionName(context) }
    val isDebug = rawVersion.endsWith("-debug")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp),
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = null,
                modifier = Modifier.size(112.dp).clip(CircleShape),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text("Boom, listener!", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AboutChip(rawVersion.removeSuffix("-debug"))
            AboutChip(
                stringResource(
                    if (isDebug) R.string.feature_settings_about_build_debug else R.string.feature_settings_about_build_release,
                ).uppercase(),
            )
        }
    }
}

@Composable
private fun AboutChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = CircleShape,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
    }
}

/** O ícone do app, desenhado num bitmap (o módulo não enxerga os recursos do :app). */
private fun appIcon(context: Context): ImageBitmap? = runCatching {
    val drawable = context.packageManager.getApplicationIcon(context.packageName)
    val size = 288
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    drawable.setBounds(0, 0, size, size)
    drawable.draw(canvas)
    bitmap.asImageBitmap()
}.getOrNull()

@Suppress("DEPRECATION")
private fun rawVersionName(context: Context): String =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"

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
    Attribution("song.link", "Odesli · links between music services"),
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
