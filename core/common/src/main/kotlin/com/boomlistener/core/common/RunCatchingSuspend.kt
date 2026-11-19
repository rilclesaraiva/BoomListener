// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.common

import kotlin.coroutines.cancellation.CancellationException

/**
 * Como `runCatching`, mas para blocos que chamam funções `suspend`: o cancelamento da corrotina
 * (sair da tela, por exemplo) é relançado em vez de virar um `Result` de falha, então a rotina
 * cancelada para na hora e não segue até a próxima suspensão.
 */
inline fun <T> runCatchingSuspend(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }
