// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/*
 * Movimento do app: tudo vem do motionScheme do Material 3 Expressive (molas),
 * nunca de durações escritas à mão. As telas usam estas funções em vez de
 * acessar o motionScheme direto, porque ele ainda é API experimental.
 */

/** Para coisas que se movem ou mudam de tamanho (painéis, telas deslizando). */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> boomSpatialSpec(): FiniteAnimationSpec<T> = MaterialTheme.motionScheme.defaultSpatialSpec()

/** Para movimentos rápidos e curtos (voltar ao lugar depois de um arraste). */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> boomFastSpatialSpec(): FiniteAnimationSpec<T> = MaterialTheme.motionScheme.fastSpatialSpec()

/** Para cor e transparência (aparecer, sumir, trocar de aba). */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> boomEffectsSpec(): FiniteAnimationSpec<T> = MaterialTheme.motionScheme.defaultEffectsSpec()
