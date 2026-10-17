// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.core.datastore.PreferencesData
import com.boomlistener.core.datastore.UserPreferences
import com.boomlistener.core.designsystem.theme.BoomTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val preferences = PreferencesData.repository(applicationContext).preferences
        setContent {
            val prefs by preferences.collectAsStateWithLifecycle(initialValue = UserPreferences())
            BoomTheme(themeMode = prefs.themeMode, dynamicColor = prefs.dynamicColor) {
                BoomListenerApp()
            }
        }
    }
}
