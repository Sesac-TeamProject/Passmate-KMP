package org.sesacteamproject.passmate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import org.sesacteamproject.passmate.component.PassmateSplash
import org.sesacteamproject.passmate.navigation.AppNavHost
import org.sesacteamproject.passmate.theme.PassmateTheme

// 스플래시(M-00) 표시 시간. 부트스트랩이 전부 동기라 대기 구간이 없어 의도적으로 유지한다
// (2026-09-03 팀 결정 — 시안의 "준비되는 즉시"와 다름). 줄일 때는 이 값만 바꾼다.
private const val SPLASH_DURATION_MS = 2_000L

// 화면에 보여줄 앱 버전. Android versionName("1.0")과 같은 값을 쓴다.
private const val APP_VERSION_LABEL = "v1.0"

@Composable
fun App() {
    var isSplashVisible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(SPLASH_DURATION_MS)
        isSplashVisible = false
    }
    PassmateTheme {
        // 스플래시는 라우트가 아니라 셸이 소유하는 오버레이다 — 뒤로가기 대상이 아니고
        // 라우트로 두면 3플랫폼 네비게이션을 모두 건드려야 한다 (규칙 §11-1)
        if (isSplashVisible) {
            PassmateSplash(versionLabel = APP_VERSION_LABEL)
        } else {
            AppNavHost()
        }
    }
}
