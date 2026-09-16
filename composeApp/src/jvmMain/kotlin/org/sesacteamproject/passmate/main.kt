package org.sesacteamproject.passmate

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.sesacteamproject.passmate.core.di.initKoin
import org.sesacteamproject.passmate.di.viewModelModule

fun main() {
    initKoin {
        modules(viewModelModule)
    }
    application {
        Window(
            onCloseRequest = ::exitApplication,
            // 본문 클램프(600) + 레일(80)이 여유 있게 들어가는 크기로 연다. 무지정 기본값은 800x600이다
            state = rememberWindowState(width = 1100.dp, height = 760.dp),
            title = "패스메이트",
            icon = painterResource("passmate-icon.png"),
        ) {
            App()
        }
    }
}
