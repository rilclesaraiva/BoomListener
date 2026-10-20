// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.data.social.NumberNetwork
import com.boomlistener.data.social.SocialAccount
import com.boomlistener.data.social.SocialNetwork
import com.boomlistener.data.social.callingCodeFor
import com.boomlistener.data.social.cleanNetworkHandle
import com.boomlistener.data.social.normalizePhone
import java.util.Locale

/**
 * Editar as redes do perfil. A pessoa digita só o @ ou o número; o app monta
 * o link (nunca um link colado). Números: código do país do celular, que dá
 * para trocar; só vão para os amigos com "Mostrar meu número aos amigos".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NetworksSheet(account: SocialAccount, viewModel: MyProfileViewModel, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val handles = remember { mutableStateMapOf<SocialNetwork, String>().apply { putAll(account.links) } }
    val defaultCode = remember { callingCodeFor(Locale.getDefault().country).orEmpty() }
    // Número guardado já tem o código do país: mostra separado para editar.
    val codes = remember {
        mutableStateMapOf<NumberNetwork, String>().apply {
            NumberNetwork.entries.forEach { network -> put(network, codeOf(account.numbers[network], defaultCode)) }
        }
    }
    val numbers = remember {
        mutableStateMapOf<NumberNetwork, String>().apply {
            account.numbers.forEach { (network, digits) ->
                val code = codes[network].orEmpty()
                // Número de outro país: mostra completo, com "+".
                put(network, if (code.isEmpty()) "+$digits" else digits.removePrefix(code))
            }
        }
    }
    var shareNumbers by remember { mutableStateOf(account.shareNumbers) }
    var showErrors by remember { mutableStateOf(false) }

    val cleanHandles = SocialNetwork.entries.associateWith { cleanNetworkHandle(it, handles[it].orEmpty()) }
    val cleanNumbers = NumberNetwork.entries.associateWith { normalizePhone(numbers[it].orEmpty(), codes[it].orEmpty()) }
    val valid = cleanHandles.values.all { it != null } && cleanNumbers.values.all { it != null }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(stringResource(R.string.feature_social_networks_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.feature_social_networks_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            SocialNetwork.entries.forEach { network ->
                val error = showErrors && cleanHandles[network] == null
                OutlinedTextField(
                    value = handles[network].orEmpty(),
                    onValueChange = { handles[network] = it.take(80) },
                    label = { Text(network.label) },
                    leadingIcon = { NetworkIcon(networkIcon(network)) },
                    prefix = { Text("@") },
                    singleLine = true,
                    isError = error,
                    supportingText = if (error) {
                        { Text(stringResource(R.string.feature_social_networks_invalid)) }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.feature_social_numbers_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            NumberNetwork.entries.forEach { network ->
                val error = showErrors && cleanNumbers[network] == null
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(bottom = 8.dp)) {
                    OutlinedTextField(
                        value = codes[network].orEmpty(),
                        onValueChange = { codes[network] = it.filter(Char::isDigit).take(4) },
                        label = { Text(stringResource(R.string.feature_social_country_code)) },
                        prefix = { Text("+") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.width(96.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = numbers[network].orEmpty(),
                        onValueChange = { numbers[network] = it.take(24) },
                        label = { Text(network.label) },
                        leadingIcon = { NetworkIcon(networkIcon(network)) },
                        singleLine = true,
                        isError = error,
                        supportingText = if (error) {
                            { Text(stringResource(R.string.feature_social_numbers_invalid)) }
                        } else {
                            null
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
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
                    Switch(checked = shareNumbers, onCheckedChange = { shareNumbers = it }, enabled = !saving)
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    showErrors = true
                    if (!valid) return@Button
                    viewModel.saveNetworks(
                        links = cleanHandles.mapNotNull { (network, handle) -> handle?.takeIf { it.isNotEmpty() }?.let { network to it } }.toMap(),
                        numbers = cleanNumbers.mapNotNull { (network, digits) -> digits?.takeIf { it.isNotEmpty() }?.let { network to it } }.toMap(),
                        shareNumbers = shareNumbers,
                        onDone = onDismiss,
                    )
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(if (saving) R.string.feature_social_saving else R.string.feature_social_save))
            }
        }
    }
}

/** O código do país de um número guardado ("5591…" com padrão "55" → "55"); vazio usa o padrão. */
private fun codeOf(digits: String?, default: String): String =
    if (digits.isNullOrEmpty() || default.isEmpty() || digits.startsWith(default)) default else ""
