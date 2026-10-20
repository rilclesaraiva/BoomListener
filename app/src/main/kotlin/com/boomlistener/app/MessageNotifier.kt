// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.app

import android.Manifest
import android.app.Activity
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import com.boomlistener.data.social.ChatMessage
import com.boomlistener.data.social.MessageBody

/**
 * Notificação de mensagem nova em Amigos, só com o app fora da tela (com ele
 * aberto, a conversa já aparece). Uma notificação por conversa; tocar abre o app.
 */
internal class MessageNotifier(private val app: Application) {
    private var startedActivities = 0
    private val manager = app.getSystemService(NotificationManager::class.java)

    init {
        app.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityStarted(activity: Activity) {
                    startedActivities++
                    // Voltou para o app: as notificações de mensagem saem.
                    manager.cancelAll()
                }

                override fun onActivityStopped(activity: Activity) {
                    startedActivities = (startedActivities - 1).coerceAtLeast(0)
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
                override fun onActivityResumed(activity: Activity) = Unit
                override fun onActivityPaused(activity: Activity) = Unit
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
                override fun onActivityDestroyed(activity: Activity) = Unit
            },
        )
    }

    fun show(message: ChatMessage) {
        if (startedActivities > 0 || message.sender == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            app.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannel()
        val text = when (val body = message.body) {
            is MessageBody.Text -> body.text
            is MessageBody.Music -> app.getString(R.string.notification_music_card, body.title)
        }
        val open = PendingIntent.getActivity(
            app,
            0,
            app.packageManager.getLaunchIntentForPackage(app.packageName) ?: Intent(app, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = Notification.Builder(app, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(message.sender?.name)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        manager.notify(message.conversationId.hashCode(), notification)
    }

    private fun ensureChannel() {
        if (manager.getNotificationChannel(CHANNEL) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, app.getString(R.string.notification_channel_messages), NotificationManager.IMPORTANCE_HIGH),
        )
    }

    private companion object {
        const val CHANNEL = "messages"
    }
}
