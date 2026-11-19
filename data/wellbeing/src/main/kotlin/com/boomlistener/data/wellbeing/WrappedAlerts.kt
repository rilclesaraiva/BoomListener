// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.wellbeing

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.Month

/**
 * Aviso de fim de ano do Wrapped: a opção (ligada por padrão) e o que já foi
 * feito em cada ano. Fica só no aparelho (preferências; entram no backup do app, como as demais preferências).
 */
interface WrappedAlertsRepository {
    val enabled: StateFlow<Boolean>
    fun setEnabled(enabled: Boolean)

    /** Último ano em que o aviso (notificação) foi enviado; 0 = nunca. */
    var notifiedYear: Int

    /** Último ano em que a pessoa dispensou o cartão do Perfil; 0 = nunca. */
    var dismissedYear: Int
}

/**
 * Chegou a hora da notificação? Em dezembro, com a opção ligada, com 30 dias de
 * escuta e sem aviso já enviado neste ano. (Fora de dezembro nunca avisa: um
 * celular que ficou desligado não manda o Wrapped de um ano que já passou.)
 */
fun wrappedAlertDue(today: LocalDate, enabled: Boolean, notifiedYear: Int, dataSince: LocalDate?): Boolean =
    enabled && today.month == Month.DECEMBER && notifiedYear != today.year && wrappedDaysLeft(dataSince, today) == 0

/**
 * Mostra o cartão "Seu Wrapped está pronto" no Perfil? Em dezembro, com a opção
 * ligada, com 30 dias de escuta, quando a notificação não saiu (sem permissão) e
 * a pessoa ainda não dispensou o cartão.
 */
fun wrappedCardVisible(today: LocalDate, enabled: Boolean, notifiedYear: Int, dismissedYear: Int, dataSince: LocalDate?): Boolean =
    wrappedAlertDue(today, enabled, notifiedYear, dataSince) && dismissedYear != today.year

internal class PrefsWrappedAlerts(context: Context) : WrappedAlertsRepository {
    private val prefs = context.getSharedPreferences("wrapped_alerts", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, true))
    override val enabled: StateFlow<Boolean> = state.asStateFlow()

    override fun setEnabled(enabled: Boolean) {
        state.value = enabled
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    override var notifiedYear: Int
        get() = prefs.getInt(KEY_NOTIFIED, 0)
        set(value) = prefs.edit().putInt(KEY_NOTIFIED, value).apply()

    override var dismissedYear: Int
        get() = prefs.getInt(KEY_DISMISSED, 0)
        set(value) = prefs.edit().putInt(KEY_DISMISSED, value).apply()

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val KEY_NOTIFIED = "notified_year"
        const val KEY_DISMISSED = "dismissed_year"
    }
}

object WrappedAlertsData {
    @Volatile
    private var instance: WrappedAlertsRepository? = null

    fun repository(context: Context): WrappedAlertsRepository =
        instance ?: synchronized(this) {
            instance ?: PrefsWrappedAlerts(context.applicationContext).also { instance = it }
        }
}
