package org.sesacteamproject.passmate.component

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun PassmateBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
