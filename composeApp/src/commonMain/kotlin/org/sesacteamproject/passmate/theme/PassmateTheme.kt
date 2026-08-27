package org.sesacteamproject.passmate.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun PassmateTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = PassmateColors.Primary,
            onPrimary = PassmateColors.Surface,
            secondary = PassmateColors.PrimaryDeep,
            onSecondary = PassmateColors.Surface,
            background = PassmateColors.BackgroundMint,
            onBackground = PassmateColors.TextPrimary,
            surface = PassmateColors.Surface,
            onSurface = PassmateColors.TextPrimary,
            onSurfaceVariant = PassmateColors.TextSecondary,
            outline = PassmateColors.Border
        ),
        content = content
    )
}
