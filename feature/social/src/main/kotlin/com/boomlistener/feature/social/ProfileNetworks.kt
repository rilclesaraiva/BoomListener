// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.social

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.data.social.NumberNetwork
import com.boomlistener.data.social.SocialAccount
import com.boomlistener.data.social.SocialNetwork
import com.boomlistener.data.social.callingCodeFor
import com.boomlistener.data.social.cleanNetworkHandle
import com.boomlistener.data.social.normalizePhone
import java.util.Locale

/** Uma rede para escolher no painel: pelo @ ou pelo número. */
internal sealed interface AnyNetwork {
    data class Handle(val network: SocialNetwork) : AnyNetwork
    data class Number(val network: NumberNetwork) : AnyNetwork
}

/**
 * As redes do "Editar perfil" enquanto a pessoa mexe: nada vai ao servidor
 * antes de tocar em Salvar. Os @ já ficam limpos e os números já com o
 * código do país (só dígitos).
 */
@Stable
internal class NetworksDraft(account: SocialAccount) {
    private val initialLinks = account.links
    private val initialNumbers = account.numbers
    private val initialShare = account.shareNumbers

    val links = mutableStateMapOf<SocialNetwork, String>().apply { putAll(account.links) }
    val numbers = mutableStateMapOf<NumberNetwork, String>().apply { putAll(account.numbers) }
    var shareNumbers by mutableStateOf(account.shareNumbers)

    val changed: Boolean
        get() = links.toMap() != initialLinks || numbers.toMap() != initialNumbers || shareNumbers != initialShare

    /** As redes adicionadas, na ordem fixa (números primeiro, como nos botões do perfil). */
    val added: List<AnyNetwork>
        get() = NumberNetwork.entries.filter { it in numbers }.map { AnyNetwork.Number(it) } +
            SocialNetwork.entries.filter { it in links }.map { AnyNetwork.Handle(it) }

    fun set(network: AnyNetwork, value: String) {
        when (network) {
            is AnyNetwork.Handle -> if (value.isEmpty()) links.remove(network.network) else links[network.network] = value
            is AnyNetwork.Number -> if (value.isEmpty()) numbers.remove(network.network) else numbers[network.network] = value
        }
    }

    fun valueOf(network: AnyNetwork): String? = when (network) {
        is AnyNetwork.Handle -> links[network.network]
        is AnyNetwork.Number -> numbers[network.network]
    }
}

/**
 * Seção "Redes" do "Editar perfil": as redes já adicionadas como pílulas numa
 * fila que desliza para o lado, e "Adicionar rede", que abre um painel pequeno
 * para escolher a rede e digitar o @ ou o número.
 */
@Composable
internal fun NetworksEditor(draft: NetworksDraft, enabled: Boolean) {
    var editing by remember { mutableStateOf<AnyNetwork?>(null) }
    var adding by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.feature_social_networks_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.feature_social_networks_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        ) {
            draft.added.forEach { network ->
                FilledTonalButton(onClick = { editing = network }, enabled = enabled) {
                    NetworkIcon(network.icon())
                    Spacer(Modifier.width(8.dp))
                    Text(network.label())
                }
            }
            OutlinedButton(onClick = { adding = true }, enabled = enabled) {
                Icon(BoomIcons.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.feature_social_network_add))
            }
        }
        if (draft.numbers.isNotEmpty()) {
            // Aviso claro antes de mostrar o número.
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(stringResource(R.string.feature_social_numbers_share), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.feature_social_numbers_warning), style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(checked = draft.shareNumbers, onCheckedChange = { draft.shareNumbers = it }, enabled = enabled)
                }
            }
        }
    }

    editing?.let { network ->
        NetworkDialog(
            initial = network,
            draft = draft,
            onDismiss = { editing = null },
        )
    }
    if (adding) {
        NetworkDialog(initial = null, draft = draft, onDismiss = { adding = false })
    }
}

/**
 * O painel pequeno de uma rede: escolher qual (só ao adicionar) e digitar o @
 * ou o número. Uma rede já adicionada pode ser removida daqui.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NetworkDialog(initial: AnyNetwork?, draft: NetworksDraft, onDismiss: () -> Unit) {
    val defaultCode = remember { callingCodeFor(Locale.getDefault().country).orEmpty() }
    var network by remember { mutableStateOf(initial) }
    val saved = initial?.let { draft.valueOf(it) }.orEmpty()
    var text by remember {
        mutableStateOf(
            when (initial) {
                is AnyNetwork.Number -> {
                    val code = codeOf(saved, defaultCode)
                    // Número de outro país: mostra completo, com "+".
                    if (code.isEmpty()) "+$saved" else saved.removePrefix(code)
                }
                else -> saved
            },
        )
    }
    var code by remember { mutableStateOf(if (initial is AnyNetwork.Number) codeOf(saved, defaultCode) else defaultCode) }
    var showError by remember { mutableStateOf(false) }

    val current = network
    val cleaned: String? = when (current) {
        is AnyNetwork.Handle -> cleanNetworkHandle(current.network, text)
        is AnyNetwork.Number -> normalizePhone(text, code)
        null -> null
    }
    // Só as que ainda não foram adicionadas (ou a que está sendo editada).
    val options = remember(draft.added, initial) {
        (NumberNetwork.entries.map { AnyNetwork.Number(it) } + SocialNetwork.entries.map { AnyNetwork.Handle(it) })
            .filter { it == initial || draft.valueOf(it) == null }
            .sortedBy { it.sortKey() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) stringResource(R.string.feature_social_network_add) else initial.label(),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (initial == null) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        options.forEach { option ->
                            FilterChip(
                                selected = option == network,
                                onClick = {
                                    network = option
                                    showError = false
                                },
                                label = { Text(option.label()) },
                                leadingIcon = { NetworkIcon(option.icon()) },
                            )
                        }
                    }
                }
                when (current) {
                    is AnyNetwork.Handle -> OutlinedTextField(
                        value = text,
                        onValueChange = {
                            text = it.take(80)
                            showError = false
                        },
                        label = { Text(current.label()) },
                        prefix = { Text("@") },
                        singleLine = true,
                        isError = showError,
                        supportingText = if (showError) {
                            { Text(stringResource(R.string.feature_social_networks_invalid)) }
                        } else {
                            null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    is AnyNetwork.Number -> {
                        Row(verticalAlignment = Alignment.Top) {
                            OutlinedTextField(
                                value = code,
                                onValueChange = { code = it.filter(Char::isDigit).take(4) },
                                label = { Text(stringResource(R.string.feature_social_country_code)) },
                                prefix = { Text("+") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.width(92.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = text,
                                onValueChange = {
                                    text = it.take(24)
                                    showError = false
                                },
                                label = { Text(current.label()) },
                                singleLine = true,
                                isError = showError,
                                supportingText = if (showError) {
                                    { Text(stringResource(R.string.feature_social_numbers_invalid)) }
                                } else {
                                    null
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Text(
                            stringResource(R.string.feature_social_numbers_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    null -> Unit
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = current != null && text.isNotBlank(),
                onClick = {
                    val value = cleaned
                    if (current == null || value.isNullOrEmpty()) {
                        showError = true
                        return@TextButton
                    }
                    // Trocou de rede no painel ao editar: tira a antiga.
                    if (initial != null && initial != current) draft.set(initial, "")
                    draft.set(current, value)
                    onDismiss()
                },
            ) {
                Text(stringResource(if (initial == null) R.string.feature_social_add else R.string.feature_social_done))
            }
        },
        dismissButton = {
            Row {
                if (initial != null) {
                    TextButton(onClick = {
                        draft.set(initial, "")
                        onDismiss()
                    }) {
                        Text(stringResource(R.string.feature_social_remove))
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_social_cancel)) }
            }
        },
    )
}

@Composable
private fun AnyNetwork.label(): String = when (this) {
    is AnyNetwork.Handle -> network.label
    // Telegram existe pelo @ e pelo número: o do número leva a indicação.
    is AnyNetwork.Number -> if (network == NumberNetwork.WhatsApp) {
        network.label
    } else {
        stringResource(R.string.feature_social_network_by_number, network.label)
    }
}

private fun AnyNetwork.icon(): Int = when (this) {
    is AnyNetwork.Handle -> networkIcon(network)
    is AnyNetwork.Number -> networkIcon(network)
}

/** Ordem alfabética no painel (Telegram pelo @ antes de Telegram pelo número). */
private fun AnyNetwork.sortKey(): String = when (this) {
    is AnyNetwork.Handle -> network.label.lowercase() + "0"
    is AnyNetwork.Number -> network.label.lowercase() + "1"
}

/** O código do país de um número guardado ("5591…" com padrão "55" → "55"); vazio usa o padrão. */
private fun codeOf(digits: String?, default: String): String =
    if (digits.isNullOrEmpty() || default.isEmpty() || digits.startsWith(default)) default else ""
