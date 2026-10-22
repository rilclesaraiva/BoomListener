// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.app

import android.content.Intent
import android.os.Bundle
import com.boomlistener.feature.releases.ReleaseAlerts
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.core.datastore.PreferencesData
import com.boomlistener.core.datastore.UserPreferences
import com.boomlistener.core.designsystem.theme.BoomTheme

/** Pedidos vindos de fora da tela (ex.: tocar no aviso de lançamento). */
internal object AppRequests {
    val openReleases = MutableStateFlow(false)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handle(intent)
        val preferences = PreferencesData.repository(applicationContext).preferences
        setContent {
            val prefs by preferences.collectAsStateWithLifecycle(initialValue = UserPreferences())
            BoomTheme(themeMode = prefs.themeMode, dynamicColor = prefs.dynamicColor) {
                BoomListenerApp()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        if (intent?.getBooleanExtra(ReleaseAlerts.EXTRA_OPEN_RELEASES, false) == true) {
            AppRequests.openReleases.value = true
            intent.removeExtra(ReleaseAlerts.EXTRA_OPEN_RELEASES)
        }
    }
}
