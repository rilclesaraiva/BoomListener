// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.releases

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
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.boomlistener.data.deezer.ArtistReleasesResult
import com.boomlistener.data.deezer.DeezerData
import com.boomlistener.data.releases.ReleasesData
import com.boomlistener.data.releases.findNewReleases
import com.boomlistener.core.model.Release
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/**
 * Avisos de lançamentos no celular: a cada ~12 h (com internet), confere a
 * discografia dos artistas seguidos no Deezer e avisa o que saiu de novo
 * ([findNewReleases] decide o que é novidade). Tocar no aviso abre a tela
 * de Lançamentos. Nada sai do aparelho além das consultas ao Deezer.
 */
object ReleaseAlerts {
    /** Extra do Intent que pede para abrir a tela de Lançamentos. */
    const val EXTRA_OPEN_RELEASES = "com.boomlistener.OPEN_RELEASES"

    private const val WORK_NAME = "release_alerts"
    private const val CHANNEL_ID = "new_releases"
    private const val SUMMARY_ID = 1
    private const val MAX_SINGLE_ALERTS = 3

    /** Agenda a conferência periódica (chamado ao abrir o app; não duplica). */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<ReleaseCheckWorker>(12, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** O app pode mostrar notificações agora? (Android 13+ pede permissão.) */
    fun canNotify(context: Context): Boolean {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    internal fun notify(context: Context, items: List<Pair<String, Release>>) {
        if (items.isEmpty() || !canNotify(context)) return
        val manager = NotificationManagerCompat.from(context)
        ensureChannel(context)
        val open = openReleasesIntent(context)
        if (items.size <= MAX_SINGLE_ALERTS) {
            items.forEach { (artist, release) ->
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification_release)
                    .setContentTitle(context.getString(R.string.feature_releases_notification_title, artist))
                    .setContentText(release.title)
                    .setContentIntent(open)
                    .setAutoCancel(true)
                    .build()
                runCatching { manager.notify(release.id.hashCode(), notification) }
            }
        } else {
            // Muitos de uma vez: um aviso só, com os nomes dentro.
            val style = NotificationCompat.InboxStyle()
            items.take(6).forEach { (artist, release) -> style.addLine("$artist — ${release.title}") }
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_release)
                .setContentTitle(context.getString(R.string.feature_releases_notification_summary, items.size))
                .setStyle(style)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            runCatching { manager.notify(SUMMARY_ID, notification) }
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.feature_releases_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.feature_releases_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun openReleasesIntent(context: Context): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(EXTRA_OPEN_RELEASES, true)
        } ?: return null
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}

/** A conferência em segundo plano (WorkManager). */
class ReleaseCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        val alerts = ReleasesData.alerts(context)
        val followed = ReleasesData.follows(context).followed.first()
        val deezer = DeezerData.repository(context)
        val fetched = followed.mapNotNull { artist ->
            (deezer.artistReleases(artist.deezerId) as? ArtistReleasesResult.Found)?.let { artist.deezerId to it.releases }
        }.toMap()
        val check = findNewReleases(followed.map { it.deezerId }.toSet(), alerts.known(), fetched, LocalDate.now())
        alerts.saveKnown(check.known)
        if (alerts.enabled.value) {
            val names = followed.associate { it.deezerId to it.name }
            ReleaseAlerts.notify(context, check.fresh.map { names[it.artistId].orEmpty() to it.release })
        }
        return Result.success()
    }
}
