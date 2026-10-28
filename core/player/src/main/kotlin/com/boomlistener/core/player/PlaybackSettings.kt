// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.player

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Ajustes de reprodução (Ajustes → Reprodução), guardados no aparelho.
 *
 * [transitionSeconds]: a música que acaba vai sumindo e a próxima vai
 * aparecendo, em tantos segundos (0 = desligado).
 * [fadeOnPlayPause]: tocar e pausar sobem/descem o volume rapidinho, sem corte seco.
 * [speed]: velocidade de reprodução (o tom da voz não muda).
 */
data class PlaybackPrefs(
    val transitionSeconds: Int = 0,
    val fadeOnPlayPause: Boolean = true,
    val speed: Float = 1f,
    /** Volume igualado pelas tags ReplayGain das músicas. */
    val replayGain: Boolean = false,
    /** Voltar a tocar quando o fone reconecta (se pausou porque ele saiu). */
    val resumeOnReconnect: Boolean = false,
    /** Ao abrir o app, o aleatório volta ligado se estava ligado. */
    val keepShuffle: Boolean = false,
    /** Letra: a da internet (LRCLIB) antes da que está dentro do arquivo. */
    val lyricsOnlineFirst: Boolean = false,
    /** Sem pausa entre faixas: corta o silêncio que o codificador põe no começo e no fim. */
    val gapless: Boolean = true,
) {
    companion object {
        const val MAX_TRANSITION_SECONDS = 12
        const val MIN_SPEED = 0.5f
        const val MAX_SPEED = 2f
    }
}

/** Timer para dormir: pausa numa hora ([endsAtMs], relógio do aparelho) ou no fim da música. */
data class SleepTimer(val endsAtMs: Long? = null, val endOfTrack: Boolean = false)

interface PlaybackSettings {
    val prefs: StateFlow<PlaybackPrefs>
    fun setTransitionSeconds(seconds: Int)
    fun setFadeOnPlayPause(enabled: Boolean)
    fun setSpeed(speed: Float)
    fun setReplayGain(enabled: Boolean)
    fun setResumeOnReconnect(enabled: Boolean)
    fun setKeepShuffle(enabled: Boolean)
    fun setLyricsOnlineFirst(enabled: Boolean)
    fun setGapless(enabled: Boolean)

    /** null = sem timer. Não é guardado: fechar o app desliga. */
    val sleepTimer: StateFlow<SleepTimer?>
    fun startSleepTimer(minutes: Int, nowMs: Long = System.currentTimeMillis())
    fun sleepAtEndOfTrack()
    fun cancelSleepTimer()
}

/** Ponto de acesso provisório enquanto o app não tem Hilt. */
object PlaybackSettingsData {
    @Volatile
    private var instance: SharedPrefsPlaybackSettings? = null

    fun get(context: Context): PlaybackSettings = internalGet(context)

    internal fun internalGet(context: Context): SharedPrefsPlaybackSettings =
        instance ?: synchronized(this) {
            instance ?: SharedPrefsPlaybackSettings(context.applicationContext).also { instance = it }
        }
}

internal class SharedPrefsPlaybackSettings(context: Context) : PlaybackSettings {
    private val store = context.getSharedPreferences("playback_settings", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(
        PlaybackPrefs(
            transitionSeconds = store.getInt(KEY_TRANSITION, 0),
            // Ligado por padrão; quem desligou nos Ajustes continua desligado.
            fadeOnPlayPause = store.getBoolean(KEY_FADE, true),
            speed = store.getFloat(KEY_SPEED, 1f),
            replayGain = store.getBoolean(KEY_REPLAY_GAIN, false),
            resumeOnReconnect = store.getBoolean(KEY_RESUME, false),
            keepShuffle = store.getBoolean(KEY_KEEP_SHUFFLE, false),
            lyricsOnlineFirst = store.getBoolean(KEY_LYRICS_ONLINE, false),
            gapless = store.getBoolean(KEY_GAPLESS, true),
        ),
    )
    override val prefs: StateFlow<PlaybackPrefs> = state.asStateFlow()

    private val sleep = MutableStateFlow<SleepTimer?>(null)
    override val sleepTimer: StateFlow<SleepTimer?> = sleep.asStateFlow()

    override fun setTransitionSeconds(seconds: Int) {
        val clean = seconds.coerceIn(0, PlaybackPrefs.MAX_TRANSITION_SECONDS)
        store.edit().putInt(KEY_TRANSITION, clean).apply()
        state.value = state.value.copy(transitionSeconds = clean)
    }

    override fun setFadeOnPlayPause(enabled: Boolean) {
        store.edit().putBoolean(KEY_FADE, enabled).apply()
        state.value = state.value.copy(fadeOnPlayPause = enabled)
    }

    override fun setSpeed(speed: Float) {
        val clean = roundSpeed(speed)
        store.edit().putFloat(KEY_SPEED, clean).apply()
        state.value = state.value.copy(speed = clean)
    }

    override fun setReplayGain(enabled: Boolean) {
        store.edit().putBoolean(KEY_REPLAY_GAIN, enabled).apply()
        state.value = state.value.copy(replayGain = enabled)
    }

    override fun setResumeOnReconnect(enabled: Boolean) {
        store.edit().putBoolean(KEY_RESUME, enabled).apply()
        state.value = state.value.copy(resumeOnReconnect = enabled)
    }

    override fun setKeepShuffle(enabled: Boolean) {
        store.edit().putBoolean(KEY_KEEP_SHUFFLE, enabled).apply()
        state.value = state.value.copy(keepShuffle = enabled)
    }

    override fun setLyricsOnlineFirst(enabled: Boolean) {
        store.edit().putBoolean(KEY_LYRICS_ONLINE, enabled).apply()
        state.value = state.value.copy(lyricsOnlineFirst = enabled)
    }

    override fun setGapless(enabled: Boolean) {
        store.edit().putBoolean(KEY_GAPLESS, enabled).apply()
        state.value = state.value.copy(gapless = enabled)
    }

    override fun startSleepTimer(minutes: Int, nowMs: Long) {
        sleep.value = SleepTimer(endsAtMs = nowMs + minutes.coerceAtLeast(1) * 60_000L)
    }

    override fun sleepAtEndOfTrack() {
        sleep.value = SleepTimer(endOfTrack = true)
    }

    override fun cancelSleepTimer() {
        sleep.value = null
    }

    private companion object {
        const val KEY_TRANSITION = "transition_s"
        const val KEY_FADE = "fade_play_pause"
        const val KEY_SPEED = "speed"
        const val KEY_REPLAY_GAIN = "replay_gain"
        const val KEY_RESUME = "resume_on_reconnect"
        const val KEY_KEEP_SHUFFLE = "keep_shuffle"
        const val KEY_LYRICS_ONLINE = "lyrics_online_first"
        const val KEY_GAPLESS = "gapless"
    }
}

/** Velocidade dentro dos limites, de 0,05 em 0,05 (0,97 vira 0,95). */
internal fun roundSpeed(speed: Float): Float =
    (Math.round(speed.coerceIn(PlaybackPrefs.MIN_SPEED, PlaybackPrefs.MAX_SPEED) * 20f) / 20f)

/**
 * Volume da transição entre músicas em [positionMs] de [durationMs]: sobe nos
 * primeiros segundos e desce nos últimos. 1 = volume normal.
 */
internal fun transitionVolume(positionMs: Long, durationMs: Long, transitionSeconds: Int): Float {
    if (transitionSeconds <= 0 || durationMs <= 0) return 1f
    // Em música curta, a transição nunca passa de um terço dela.
    val fadeMs = minOf(transitionSeconds * 1000L, durationMs / 3).coerceAtLeast(1)
    val fadeIn = positionMs.toFloat() / fadeMs
    val fadeOut = (durationMs - positionMs).toFloat() / fadeMs
    return minOf(1f, fadeIn, fadeOut).coerceIn(0f, 1f)
}
