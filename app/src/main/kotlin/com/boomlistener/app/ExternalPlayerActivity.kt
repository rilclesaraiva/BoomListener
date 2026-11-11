// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.boomlistener.core.datastore.PreferencesData
import com.boomlistener.core.datastore.UserPreferences
import com.boomlistener.core.designsystem.theme.BoomTheme
import com.boomlistener.core.player.PlayerConnection
import com.boomlistener.data.library.LibraryData
import com.boomlistener.feature.nowplaying.ExternalPlayerPopup
import kotlinx.coroutines.launch

/**
 * "Abrir com" o BoomListener num arquivo de áudio: toca o arquivo e mostra um
 * player pequeno por cima do app de onde veio (sem abrir o app inteiro).
 * A música continua tocando ao fechar o painel.
 */
class ExternalPlayerActivity : ComponentActivity() {
    // A música deste pedido; o painel só aparece quando é ela que está tocando.
    private var trackUri by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handle(intent)
        val preferences = PreferencesData.repository(applicationContext).preferences
        setContent {
            val prefs by preferences.collectAsStateWithLifecycle(initialValue = UserPreferences())
            BoomTheme(themeMode = prefs.themeMode, dynamicColor = prefs.dynamicColor) {
                ExternalPlayerPopup(
                    trackUri = trackUri,
                    onOpenApp = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    },
                    onClose = ::finish,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        val audio = intent?.data ?: return finish()
        lifecycleScope.launch {
            val track = LibraryData.repository(applicationContext).trackFromUri(audio.toString())
            if (track == null) {
                Toast.makeText(applicationContext, R.string.app_open_file_failed, Toast.LENGTH_LONG).show()
                finish()
            } else {
                PlayerConnection.controller(applicationContext).play(listOf(track), 0)
                trackUri = track.uri
            }
        }
    }
}
