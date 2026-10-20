// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.social

import org.unifiedpush.android.connector.FailedReason
import org.unifiedpush.android.connector.PushService
import org.unifiedpush.android.connector.data.PushEndpoint
import org.unifiedpush.android.connector.data.PushMessage

/**
 * Recebe os avisos do UnifiedPush (pelo app distribuidor, ex.: ntfy). O aviso
 * só diz "acorde": o app conecta por alguns segundos, entrega o que estava
 * esperando e busca as mensagens novas. Nada do conteúdo passa pelo push.
 */
internal class SocialPushService : PushService() {
    private val repository: HttpSocialRepository?
        get() = SocialData.repository(applicationContext) as? HttpSocialRepository

    override fun onNewEndpoint(endpoint: PushEndpoint, instance: String) {
        repository?.onPushEndpoint(endpoint.url)
    }

    override fun onMessage(message: PushMessage, instance: String) {
        repository?.wakeFromPush()
    }

    override fun onRegistrationFailed(reason: FailedReason, instance: String) {
        repository?.onPushEndpoint(null)
    }

    override fun onUnregistered(instance: String) {
        repository?.onPushEndpoint(null)
    }
}
