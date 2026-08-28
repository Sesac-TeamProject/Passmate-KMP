package org.sesacteamproject.passmate.ui.play

import androidx.compose.runtime.Composable

enum class VoiceHintPlaybackState {
    IDLE,
    PLAYING,
    PAUSED,
    ENDED,
    FAILED
}

// 음성 힌트 클립 재생 제어 — 플랫폼 구현은 expect/actual 뒤로 숨긴다 (규칙 §2)
interface VoiceHintPlayerController {

    fun play(url: String)

    fun pause()

    fun resume()

    fun stop()

    fun playbackState(): VoiceHintPlaybackState

    fun positionMillis(): Long
}

// Android=Media3 ExoPlayer, Desktop=미지원(null 반환 시 수동 안내 칩만 표시)
@Composable
expect fun rememberVoiceHintPlayer(): VoiceHintPlayerController?
