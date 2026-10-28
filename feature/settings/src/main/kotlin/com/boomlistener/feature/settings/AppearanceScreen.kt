// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.settings

import android.os.Build
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.boomlistener.core.datastore.PreferencesData
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.designsystem.component.BoomPageTitle
import com.boomlistener.core.designsystem.component.BoomScreenScaffold
import com.boomlistener.core.designsystem.component.BoomSettingsItem
import com.boomlistener.core.designsystem.component.BoomSettingsSection
import com.boomlistener.core.model.ThemeMode
import com.boomlistener.core.player.PlaybackSettingsData
import com.boomlistener.core.player.VisualizerStyle

@Composable
internal fun AppearanceScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    val viewModel: AppearanceViewModel = viewModel {
        AppearanceViewModel(PreferencesData.repository(context))
    }
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val playback = remember { PlaybackSettingsData.get(context) }
    val playbackPrefs by playback.prefs.collectAsStateWithLifecycle()
    val visualizerStyles = listOf(
        Triple(VisualizerStyle.Bars, R.string.feature_settings_visualizer_bars, R.string.feature_settings_visualizer_bars_summary),
        Triple(VisualizerStyle.Wave, R.string.feature_settings_visualizer_wave, R.string.feature_settings_visualizer_wave_summary),
    )

    val themes = listOf(
        Triple(ThemeMode.System, R.string.feature_settings_theme_system, R.string.feature_settings_theme_system_summary),
        Triple(ThemeMode.Light, R.string.feature_settings_theme_light, null),
        Triple(ThemeMode.Dark, R.string.feature_settings_theme_dark, null),
        Triple(ThemeMode.Amoled, R.string.feature_settings_theme_amoled, R.string.feature_settings_theme_amoled_summary),
    )

    BoomScreenScaffold(
        title = stringResource(R.string.feature_settings_appearance),
        onBack = onBack,
        modifier = modifier,
        // Sem barra presa na tela: o título é o topo da página.
        topBar = {},
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            item(key = "pageTitle") {
                BoomPageTitle(stringResource(R.string.feature_settings_appearance), hasBack = true, large = true)
            }
            item {
                BoomSettingsSection(title = stringResource(R.string.feature_settings_theme)) {
                    themes.forEach { (mode, title, summary) ->
                        BoomSettingsItem(
                            icon = null,
                            title = stringResource(title),
                            subtitle = summary?.let { stringResource(it) },
                            onClick = { viewModel.setThemeMode(mode) },
                            trailing = {
                                RadioButton(
                                    selected = prefs.themeMode == mode,
                                    onClick = { viewModel.setThemeMode(mode) },
                                )
                            },
                        )
                    }
                }
            }
            item {
                BoomSettingsSection(title = stringResource(R.string.feature_settings_colors)) {
                    BoomSettingsItem(
                        icon = null,
                        title = stringResource(R.string.feature_settings_dynamic_color),
                        subtitle = stringResource(
                            if (dynamicAvailable) R.string.feature_settings_dynamic_color_summary
                            else R.string.feature_settings_dynamic_color_unavailable,
                        ),
                        enabled = dynamicAvailable,
                        onClick = { viewModel.setDynamicColor(!prefs.dynamicColor) },
                        trailing = {
                            Switch(
                                checked = dynamicAvailable && prefs.dynamicColor,
                                onCheckedChange = { viewModel.setDynamicColor(it) },
                                enabled = dynamicAvailable,
                            )
                        },
                    )
                }
            }
            item {
                // Idioma: inglês, português ou espanhol. O Android 13+ tem a escolha
                // por app nos ajustes do sistema; antes disso, segue o idioma do celular.
                val perAppLanguage = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                BoomSettingsSection(title = stringResource(R.string.feature_settings_language)) {
                    BoomSettingsItem(
                        icon = null,
                        title = stringResource(R.string.feature_settings_language_app),
                        subtitle = if (perAppLanguage) {
                            java.util.Locale.getDefault().let { it.getDisplayLanguage(it).replaceFirstChar(Char::titlecase) }
                        } else {
                            stringResource(R.string.feature_settings_language_follows_phone)
                        },
                        enabled = perAppLanguage,
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    android.content.Intent(android.provider.Settings.ACTION_APP_LOCALE_SETTINGS)
                                        .setData(android.net.Uri.fromParts("package", context.packageName, null))
                                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            }
                        },
                    )
                }
            }
            item {
                // Tipo do visualizador sobre a capa (liga e desliga no menu ⋮ do player).
                BoomSettingsSection(title = stringResource(R.string.feature_settings_visualizer)) {
                    visualizerStyles.forEach { (style, title, summary) ->
                        BoomSettingsItem(
                            // Desenho pequeno de como ele fica: barrinhas ou a onda em mola.
                            icon = if (style == VisualizerStyle.Bars) BoomIcons.GraphicEq else BoomIcons.Waveform,
                            title = stringResource(title),
                            subtitle = stringResource(summary),
                            onClick = { playback.setVisualizerStyle(style) },
                            trailing = {
                                RadioButton(
                                    selected = playbackPrefs.visualizerStyle == style,
                                    onClick = { playback.setVisualizerStyle(style) },
                                )
                            },
                        )
                    }
                }
            }
            item {
                BoomSettingsSection(title = stringResource(R.string.feature_settings_effects)) {
                    // Desfoque da tela de trás (o Android só desfoca a partir do 12).
                    BoomSettingsItem(
                        icon = null,
                        title = stringResource(R.string.feature_settings_background_blur),
                        subtitle = stringResource(
                            if (dynamicAvailable) R.string.feature_settings_background_blur_summary
                            else R.string.feature_settings_background_blur_unavailable,
                        ),
                        enabled = dynamicAvailable,
                        onClick = { viewModel.setBackgroundBlur(!prefs.backgroundBlur) },
                        trailing = {
                            Switch(
                                checked = dynamicAvailable && prefs.backgroundBlur,
                                onCheckedChange = { viewModel.setBackgroundBlur(it) },
                                enabled = dynamicAvailable,
                            )
                        },
                    )
                }
            }
        }
    }
}
