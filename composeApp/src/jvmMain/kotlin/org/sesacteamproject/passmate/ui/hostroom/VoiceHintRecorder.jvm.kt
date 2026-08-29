package org.sesacteamproject.passmate.ui.hostroom

import androidx.compose.runtime.Composable

// Desktop은 PTT 녹음 미지원 — 리모컨에 안내 칩만 표시한다 (VoiceHintPlayer.jvm과 동일 정책)
@Composable
actual fun rememberVoiceHintRecorder(): VoiceHintRecorderController? {
    return null
}
