package org.sesacteamproject.passmate

import androidx.compose.runtime.Composable
import org.sesacteamproject.passmate.navigation.AppNavHost
import org.sesacteamproject.passmate.theme.PassmateTheme

@Composable
fun App() {
    PassmateTheme {
        AppNavHost()
    }
}
