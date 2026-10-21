// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.core.designsystem.component.BoomMenuAction
import com.boomlistener.data.library.FavoritesData
import kotlinx.coroutines.launch

/** "Favoritar" / "Tirar dos favoritos" no menu de uma música. */
@Composable
internal fun favoriteAction(uri: String): BoomMenuAction {
    val context = LocalContext.current.applicationContext
    val repository = remember { FavoritesData.repository(context) }
    val favorites by repository.favorites.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val favorite = uri in favorites
    return BoomMenuAction(
        stringResource(if (favorite) R.string.feature_nowplaying_unfavorite else R.string.feature_nowplaying_favorite),
    ) { scope.launch { repository.toggle(uri) } }
}
