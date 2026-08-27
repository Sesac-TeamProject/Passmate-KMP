package org.sesacteamproject.passmate

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.sesacteamproject.passmate.core.di.initKoin
import org.sesacteamproject.passmate.di.viewModelModule

fun main() {
    initKoin {
        modules(viewModelModule)
    }
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Passmate",
        ) {
            App()
        }
    }
}
