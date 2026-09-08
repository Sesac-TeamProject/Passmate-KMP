package org.sesacteamproject.passmate.component

import androidx.compose.runtime.Composable

// Desktop에는 시스템 뒤로가기가 없다 — 라우트 이동은 화면 안의 버튼(NavigateBack)뿐이다
@Composable
actual fun PassmateBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // no-op
}
