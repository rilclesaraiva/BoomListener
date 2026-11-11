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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * "Abrir com" o BoomListener num arquivo de áudio: um player pequeno por cima
 * do app de onde veio o arquivo, com player próprio. A fila e a música do app
 * não são tocadas. Sair do pop-up (fechar, ir para outro app) encerra o som dele.
 */
class ExternalPlayerActivity : ComponentActivity() {
    private lateinit var player: ExternalPlayer
    private var track by mutableStateOf<Track?>(null)

    // O player do app estava tocando quando o pop-up começou? (null = ainda não conferido.)
    // O Android o pausa para o pop-up tocar; ao fechar, só volta a tocar se era ele que estava tocando.
    private var appWasPlaying: Boolean? = null
    private var handedOver = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        player = ExternalPlayer(applicationContext)
        // Já começa a conectar ao player do app: quando o arquivo for lido, o estado dele está certo.
        PlayerConnection.controller(applicationContext)
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
        // Fechou o pop-up (não girou a tela nem passou a música ao app): devolve o que ele pausou.
        if (isFinishing && !handedOver && appWasPlaying == true) {
            val controller = PlayerConnection.controller(applicationContext)
            val app = controller.state.value
            if (app.item != null && !app.isPlaying) controller.togglePlayPause()
        }
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
                // Só na primeira vez: um segundo arquivo chega com o app já pausado por este pop-up.
                if (appWasPlaying == null) {
                    val controller = PlayerConnection.controller(applicationContext)
                    // Sem nada na fila pode ser só o app ainda conectando ao serviço: espera um instante.
                    if (controller.state.value.item == null) withTimeoutOrNull(300) { controller.state.first { it.item != null } }
                    appWasPlaying = controller.state.value.isPlaying
                }
                track = found
                player.play(found.uri)
            }
        }
    }

    /** "Abrir no BoomListener": só aqui a música passa para o player do app (pedido da pessoa). */
    private fun openInApp() {
        val current = track ?: return
        if (handedOver) return
        handedOver = true
        // Continua do ponto em que estava no pop-up, não do começo.
        val position = player.currentPositionMs()
        player.release()
        lifecycleScope.launch {
            // Arquivo de outro app: leva uma cópia, porque a permissão de leitura dele acaba com este pop-up.
            val kept = LibraryData.repository(applicationContext).keepExternalCopy(current)
            val controller = PlayerConnection.controller(applicationContext)
            controller.play(listOf(kept), 0)
            controller.seekTo(position)
            // NEW_TASK: o app abre na tarefa dele. Sem isto ele entrava na tarefa do pop-up e, da vez
            // seguinte, o pop-up (mesma tarefa) abria por cima do app em vez de por cima do gerenciador.
            startActivity(Intent(this@ExternalPlayerActivity, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            finish()
        }
    }
}
