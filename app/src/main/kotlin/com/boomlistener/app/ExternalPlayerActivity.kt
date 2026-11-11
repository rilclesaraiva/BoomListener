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
import com.boomlistener.core.model.Track
import com.boomlistener.core.player.ExternalPlayer
import com.boomlistener.core.player.PlayerConnection
import com.boomlistener.data.library.LibraryData
import com.boomlistener.feature.nowplaying.ExternalPlayerPopup
import kotlinx.coroutines.launch

/**
 * "Abrir com" o BoomListener num arquivo de áudio: um player pequeno por cima
 * do app de onde veio o arquivo, com player próprio. A fila e a música do app
 * não são tocadas. Sair do pop-up (fechar, ir para outro app) encerra o som dele.
 */
class ExternalPlayerActivity : ComponentActivity() {
    private lateinit var player: ExternalPlayer
    private var track by mutableStateOf<Track?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        player = ExternalPlayer(applicationContext)
        handle(intent)
        val preferences = PreferencesData.repository(applicationContext).preferences
        setContent {
            val prefs by preferences.collectAsStateWithLifecycle(initialValue = UserPreferences())
            BoomTheme(themeMode = prefs.themeMode, dynamicColor = prefs.dynamicColor) {
                ExternalPlayerPopup(
                    track = track,
                    player = player,
                    onOpenApp = ::openInApp,
                    onClose = ::finish,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    // Saiu da frente (outro app, tela apagada): o pop-up não fica tocando sem controle na tela.
    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) finish()
    }

    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }

    private fun handle(intent: Intent?) {
        val audio = intent?.data ?: return finish()
        lifecycleScope.launch {
            val found = LibraryData.repository(applicationContext).trackFromUri(audio.toString())
            if (found == null) {
                Toast.makeText(applicationContext, R.string.app_open_file_failed, Toast.LENGTH_LONG).show()
                finish()
            } else {
                track = found
                player.play(found.uri)
            }
        }
    }

    /** "Abrir no BoomListener": só aqui a música passa para o player do app (pedido da pessoa). */
    private fun openInApp() {
        val current = track ?: return
        // Continua do ponto em que estava no pop-up, não do começo.
        val position = player.currentPositionMs()
        player.release()
        val controller = PlayerConnection.controller(applicationContext)
        controller.play(listOf(current), 0)
        controller.seekTo(position)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
