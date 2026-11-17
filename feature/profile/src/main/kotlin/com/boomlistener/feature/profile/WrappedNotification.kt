// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.profile

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.boomlistener.data.wellbeing.DailyStatsData
import com.boomlistener.data.wellbeing.WrappedAlertsData
import com.boomlistener.data.wellbeing.firstDay
import com.boomlistener.data.wellbeing.wrappedAlertDue
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Aviso de fim de ano: em dezembro, com a opção ligada e 30 dias de escuta, uma
 * notificação "Seu Wrapped está pronto" (uma vez por ano). Só sai se o app já
 * tem a permissão de notificação: o Wrapped nunca a pede. Sem ela, o Perfil
 * mostra um cartão no lugar. Tocar no aviso abre o Wrapped do ano. Nada sai do aparelho.
 */
object WrappedNotification {
    /** Extra do Intent que pede para abrir o Wrapped do ano. */
    const val EXTRA_OPEN_WRAPPED = "com.boomlistener.OPEN_WRAPPED"

    private const val WORK_NAME = "wrapped_alert"
    private const val CHANNEL_ID = "wrapped"
    private const val NOTIFICATION_ID = 2

    /** Agenda a conferência diária (chamado ao abrir o app; não duplica). */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<WrappedAlertWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    internal fun canNotify(context: Context): Boolean {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** Envia o aviso; devolve se saiu. */
    internal fun notify(context: Context, year: Int): Boolean {
        if (!canNotify(context)) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.feature_profile_wrapped_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.feature_profile_wrapped_channel_description) }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(EXTRA_OPEN_WRAPPED, true)
        }?.let { PendingIntent.getActivity(context, 1, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE) }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_wrapped)
            .setContentTitle(context.getString(R.string.feature_profile_wrapped_notification_title, year))
            .setContentText(context.getString(R.string.feature_profile_wrapped_notification_text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        return runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }.isSuccess
    }
}

/** A conferência diária em segundo plano (WorkManager). */
class WrappedAlertWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        val alerts = WrappedAlertsData.repository(context)
        val today = LocalDate.now()
        val since = firstDay(DailyStatsData.repository(context).summary.first())
        if (wrappedAlertDue(today, alerts.enabled.value, alerts.notifiedYear, since) && WrappedNotification.notify(context, today.year)) {
            alerts.notifiedYear = today.year
        }
        return Result.success()
    }
}
