// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.releases

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.data.releases.ReleasesData

/**
 * Sino da tela de Lançamentos: liga e desliga os avisos no celular. No
 * Android 13+ pede a permissão de notificação (uma vez sozinho, ao abrir a
 * tela pela primeira vez; depois só quando a pessoa toca no sino).
 */
@Composable
internal fun ReleaseAlertsButton(onMessage: (String) -> Unit) {
    val context = LocalContext.current
    val alerts = remember { ReleasesData.alerts(context.applicationContext) }
    val enabled by alerts.enabled.collectAsStateWithLifecycle()
    var canNotify by remember { mutableStateOf(ReleaseAlerts.canNotify(context)) }
    val on = enabled && canNotify
    val turnedOn = stringResource(R.string.feature_releases_alerts_turned_on)
    val turnedOff = stringResource(R.string.feature_releases_alerts_turned_off)
    val denied = stringResource(R.string.feature_releases_alerts_denied)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        canNotify = ReleaseAlerts.canNotify(context)
        if (granted && canNotify) {
            alerts.setEnabled(true)
            onMessage(turnedOn)
        } else {
            onMessage(denied)
        }
    }
    val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    LaunchedEffect(Unit) {
        if (enabled && !canNotify && needsPermission && !alerts.askedPermission) {
            alerts.askedPermission = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    IconButton(
        onClick = {
            canNotify = ReleaseAlerts.canNotify(context)
            when {
                enabled && canNotify -> {
                    alerts.setEnabled(false)
                    onMessage(turnedOff)
                }
                canNotify -> {
                    alerts.setEnabled(true)
                    onMessage(turnedOn)
                }
                needsPermission -> {
                    alerts.askedPermission = true
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> onMessage(denied)
            }
        },
    ) {
        if (on) {
            Icon(BoomIcons.Releases, contentDescription = stringResource(R.string.feature_releases_alerts_on))
        } else {
            Icon(BoomIcons.NotificationsOff, contentDescription = stringResource(R.string.feature_releases_alerts_off))
        }
    }
}
