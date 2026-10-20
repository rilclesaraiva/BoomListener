// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.social

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

/** Pacote do ntfy (gratuito, código aberto, sem Google): o distribuidor sugerido. */
private const val NTFY_PACKAGE = "io.heckel.ntfy"

/**
 * "Mensagens com o app fechado": liga o UnifiedPush com o app distribuidor
 * instalado (ex.: ntfy). Sem nenhum, oferece instalar o ntfy. Quem não quiser
 * continua usando tudo; só não recebe aviso com o app fechado.
 */
@Composable
internal fun PushRow(
    enabled: Boolean,
    distributors: () -> List<String>,
    onEnable: (String) -> Unit,
    onDisable: () -> Unit,
) {
    val context = LocalContext.current
    var available by remember { mutableStateOf(distributors()) }
    var choosing by remember { mutableStateOf(false) }
    // Voltou da loja depois de instalar o ntfy: confere de novo.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { available = distributors() }
    }
    val start: () -> Unit = {
        when (available.size) {
            0 -> Unit
            1 -> onEnable(available.first())
            else -> choosing = true
        }
    }
    // Android 13+: pergunta se pode mostrar notificação (o aviso de mensagem nova).
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { start() }
    val turnOn: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            start()
        }
    }

    ListItem(
        modifier = Modifier.clickable {
            when {
                enabled -> onDisable()
                available.isEmpty() -> installNtfy(context)
                else -> turnOn()
            }
        },
        headlineContent = { Text(stringResource(R.string.feature_social_push_title)) },
        supportingContent = {
            Text(
                when {
                    enabled -> stringResource(R.string.feature_social_push_on)
                    available.isEmpty() -> stringResource(R.string.feature_social_push_install_hint)
                    else -> stringResource(R.string.feature_social_push_off)
                },
            )
        },
        trailingContent = {
            if (!enabled && available.isEmpty()) {
                TextButton(onClick = { installNtfy(context) }) { Text(stringResource(R.string.feature_social_push_install)) }
            } else {
                Switch(checked = enabled, onCheckedChange = { on -> if (on) turnOn() else onDisable() })
            }
        },
    )

    if (choosing) {
        AlertDialog(
            onDismissRequest = { choosing = false },
            title = { Text(stringResource(R.string.feature_social_push_choose)) },
            text = {
                Column {
                    available.forEach { pkg ->
                        ListItem(
                            headlineContent = { Text(appLabel(context, pkg)) },
                            modifier = Modifier.clickable {
                                choosing = false
                                onEnable(pkg)
                            },
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { choosing = false }) { Text(stringResource(R.string.feature_social_cancel)) }
            },
        )
    }
}

private fun appLabel(context: Context, pkg: String): String = runCatching {
    val pm = context.packageManager
    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
}.getOrDefault(pkg)

/** Abre o ntfy na loja do aparelho (Play Store, F-Droid…); sem loja, a página do F-Droid. */
private fun installNtfy(context: Context) {
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$NTFY_PACKAGE"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(market) }.onFailure {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://f-droid.org/packages/$NTFY_PACKAGE/"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}
