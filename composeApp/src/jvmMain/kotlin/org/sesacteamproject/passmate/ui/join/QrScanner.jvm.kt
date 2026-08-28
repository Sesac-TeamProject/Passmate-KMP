package org.sesacteamproject.passmate.ui.join

import androidx.compose.runtime.Composable

// Desktop은 카메라 QR 스캔 미지원 — PIN 직접 입력만 제공한다
@Composable
actual fun rememberQrScanLauncher(onResult: (String?) -> Unit): (() -> Unit)? {
    return null
}
