// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.social

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.boomlistener.core.common.openUrl
import com.boomlistener.data.social.NumberNetwork
import com.boomlistener.data.social.SocialNetwork
import com.boomlistener.data.social.networkUrl
import com.boomlistener.data.social.numberUrl

/** Ícone de cada rede (arquivos `ic_network_*.xml`: provisórios, até chegarem as logos oficiais). */
@DrawableRes
internal fun networkIcon(network: SocialNetwork): Int = when (network) {
    SocialNetwork.Instagram -> R.drawable.ic_network_instagram
    SocialNetwork.X -> R.drawable.ic_network_x
    SocialNetwork.TikTok -> R.drawable.ic_network_tiktok
    SocialNetwork.Threads -> R.drawable.ic_network_threads
    SocialNetwork.Bluesky -> R.drawable.ic_network_bluesky
    SocialNetwork.YouTube -> R.drawable.ic_network_youtube
    SocialNetwork.LastFm -> R.drawable.ic_network_lastfm
    SocialNetwork.SoundCloud -> R.drawable.ic_network_soundcloud
    SocialNetwork.Bandcamp -> R.drawable.ic_network_bandcamp
    SocialNetwork.Spotify -> R.drawable.ic_network_spotify
    SocialNetwork.Telegram -> R.drawable.ic_network_telegram
}

@DrawableRes
internal fun networkIcon(network: NumberNetwork): Int = when (network) {
    NumberNetwork.WhatsApp -> R.drawable.ic_network_whatsapp
    NumberNetwork.Telegram -> R.drawable.ic_network_telegram
}

/**
 * Um ícone de rede. Os provisórios são de uma cor só e seguem a cor do texto;
 * se as logos oficiais forem coloridas, troque [tint] para `Color.Unspecified` aqui.
 */
@Composable
internal fun NetworkIcon(@DrawableRes icon: Int, modifier: Modifier = Modifier) {
    Icon(painterResource(icon), contentDescription = null, tint = LocalContentColor.current, modifier = modifier.size(18.dp))
}

/**
 * Os botões das redes de um perfil: só as que a pessoa preencheu. O link é
 * montado pelo app a partir do @ ou do número (nunca um link de fora).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NetworkButtons(
    links: Map<SocialNetwork, String>,
    numbers: Map<NumberNetwork, String>,
    modifier: Modifier = Modifier,
) {
    if (links.isEmpty() && numbers.isEmpty()) return
    val context = LocalContext.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        numbers.forEach { (network, digits) ->
            FilledTonalButton(onClick = { context.openUrl(numberUrl(network, digits)) }) {
                NetworkIcon(networkIcon(network))
                Spacer(Modifier.width(8.dp))
                Text(network.label)
            }
        }
        SocialNetwork.entries.filter { it in links }.forEach { network ->
            FilledTonalButton(onClick = { context.openUrl(networkUrl(network, links.getValue(network))) }) {
                NetworkIcon(networkIcon(network))
                Spacer(Modifier.width(8.dp))
                Text(network.label)
            }
        }
    }
}
