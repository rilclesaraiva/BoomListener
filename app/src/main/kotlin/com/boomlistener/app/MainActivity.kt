// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.app

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import com.boomlistener.core.common.DiagnosticLog
import kotlinx.coroutines.launch
import com.boomlistener.feature.profile.WrappedNotification
import com.boomlistener.feature.releases.ReleaseAlerts
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.core.datastore.PreferencesData
import com.boomlistener.core.datastore.UserPreferences
import com.boomlistener.core.designsystem.component.BoomStatusBarScrim
import com.boomlistener.core.designsystem.theme.BoomTheme

/** Pedidos vindos de fora da tela (ex.: tocar no aviso de lançamento). */
internal object AppRequests {
    val openReleases = MutableStateFlow(false)
    val openWrapped = MutableStateFlow(false)
}

/** Menor largura (dp) a partir da qual a tela é de tablet (a mesma divisa do Android). */
private const val PHONE_MAX_WIDTH_DP = 600

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // TEMPORÁRIO (registro do "voltar"): este é o primeiro callback registrado, então é o último a
        // ser chamado: se ele rodar, ninguém no app tratou o voltar e o sistema vai fechar a tela.
        // Só fica ligado com o registro ligado; fora disso o voltar é o de sempre.
        val lastResort = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                DiagnosticLog.log("voltar", "ninguém tratou o voltar: a tela vai fechar")
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
        onBackPressedDispatcher.addCallback(this, lastResort)
        lifecycleScope.launch { DiagnosticLog.enabled.collect { lastResort.isEnabled = it } }
        // Celular fica sempre em pé (o layout é o do retrato). Tablet e
        // dobrável aberto giram à vontade; dobrar ou abrir recria a tela e
        // isto é decidido de novo.
        requestedOrientation = if (resources.configuration.smallestScreenWidthDp < PHONE_MAX_WIDTH_DP) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        if (savedInstanceState == null) handle(intent)
        val preferences = PreferencesData.repository(applicationContext).preferences
        setContent {
            val prefs by preferences.collectAsStateWithLifecycle(initialValue = UserPreferences())
            BoomTheme(themeMode = prefs.themeMode, dynamicColor = prefs.dynamicColor) {
                androidx.compose.foundation.layout.Box {
                    BoomListenerApp()
                    // Por cima de tudo: escurece de leve a barra de status (o conteúdo passa por trás dela).
                    BoomStatusBarScrim(Modifier.align(androidx.compose.ui.Alignment.TopStart))
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    // TEMPORÁRIO (registro do "voltar"): separa "o app fechou" de "foi para o fundo".
    override fun onStop() {
        super.onStop()
        DiagnosticLog.log("ciclo", "tela parou (fechando=$isFinishing)")
    }

    override fun onDestroy() {
        DiagnosticLog.log("ciclo", "tela destruída (fechando=$isFinishing)")
        super.onDestroy()
    }

    private fun handle(intent: Intent?) {
        if (intent?.getBooleanExtra(ReleaseAlerts.EXTRA_OPEN_RELEASES, false) == true) {
            AppRequests.openReleases.value = true
            intent.removeExtra(ReleaseAlerts.EXTRA_OPEN_RELEASES)
        }
        if (intent?.getBooleanExtra(WrappedNotification.EXTRA_OPEN_WRAPPED, false) == true) {
            AppRequests.openWrapped.value = true
            intent.removeExtra(WrappedNotification.EXTRA_OPEN_WRAPPED)
        }
    }
}
