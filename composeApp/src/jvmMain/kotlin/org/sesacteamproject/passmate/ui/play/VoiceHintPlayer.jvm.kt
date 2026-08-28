package org.sesacteamproject.passmate.ui.play

import androidx.compose.runtime.Composable

// Desktop은 opus/webm 오디오 재생 미지원 — 배너에 수동 안내만 표시한다
@Composable
actual fun rememberVoiceHintPlayer(): VoiceHintPlayerController? {
    return null
}
