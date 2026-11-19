// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Cor do círculo do ícone de cada item dos Ajustes. Famílias de matiz fixas (não seguem a cor
 * dinâmica, que deixava tudo parecido): itens de uma mesma categoria usam a mesma família e
 * itens de assuntos diferentes usam famílias diferentes. Regra no CLAUDE.md.
 */
enum class BoomAccent(
    internal val lightContainer: Color,
    internal val lightContent: Color,
    internal val darkContainer: Color,
    internal val darkContent: Color,
) {
    Blue(Color(0xFFD3E3FD), Color(0xFF041E49), Color(0xFF1F3A5F), Color(0xFFA8C7FA)),
    Green(Color(0xFFC4EED0), Color(0xFF072711), Color(0xFF1E4D2B), Color(0xFF9FD8AE)),
    Red(Color(0xFFF9DEDC), Color(0xFF410E0B), Color(0xFF5C1A17), Color(0xFFF2B8B5)),
    Orange(Color(0xFFFFE0B2), Color(0xFF3E2700), Color(0xFF5A3A00), Color(0xFFFFCC80)),
    Yellow(Color(0xFFFFF0B3), Color(0xFF3A3000), Color(0xFF524500), Color(0xFFFFE082)),
    Purple(Color(0xFFE8DEF8), Color(0xFF21005D), Color(0xFF3F2A73), Color(0xFFD0BCFF)),
    Pink(Color(0xFFFFD8E4), Color(0xFF3E001D), Color(0xFF5E1A38), Color(0xFFFFB1C8)),
    Teal(Color(0xFFBFF0EA), Color(0xFF00201E), Color(0xFF124A46), Color(0xFF8FDDD5)),
    Neutral(Color(0xFFE0E0E0), Color(0xFF3C3C3C), Color(0xFF3A3A3A), Color(0xFFD0D0D0)),
}

@Composable
private fun BoomAccent.colors(): Pair<Color, Color> {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return if (dark) darkContainer to darkContent else lightContainer to lightContent
}

/**
 * Grupo de itens dos Ajustes: título da seção em cima e os itens juntos num
 * bloco de cantos arredondados, separados por uma fresta (estilo Expressive).
 */
@Composable
fun BoomSettingsSection(
    title: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (title != null) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            )
        }
        Column(
            modifier = Modifier.clip(MaterialTheme.shapes.extraLarge),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            content = content,
        )
    }
}

/**
 * Um item dos Ajustes: ícone num círculo colorido, título, descrição e,
 * opcionalmente, algo à direita (um switch, um valor).
 */
@Composable
fun BoomSettingsItem(
    icon: ImageVector?,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    accent: BoomAccent = BoomAccent.Neutral,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    /** Foto no lugar do ícone (no mesmo círculo), quando houver. */
    image: ImageBitmap? = null,
) {
    val (container, onContainer) = accent.colors()
    val clickable = if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth().then(clickable),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            if (icon != null || image != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(container),
                ) {
                    if (image != null) {
                        Image(
                            bitmap = image,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize(),
                        )
                    } else if (icon != null) {
                        Icon(icon, contentDescription = null, tint = onContainer)
                    }
                }
                Spacer(Modifier.width(16.dp))
            }
            Column(Modifier.weight(1f)) {
                val alpha = if (enabled) 1f else 0.38f
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                    )
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(12.dp))
                trailing()
            }
        }
    }
}
