package org.sesacteamproject.passmate.ui.join

import androidx.compose.runtime.Composable

// QR 스캔 진입 — Android=zxing CaptureActivity, Desktop=미지원(null 반환 시 QR 버튼 숨김)
@Composable
expect fun rememberQrScanLauncher(onResult: (String?) -> Unit): (() -> Unit)?
