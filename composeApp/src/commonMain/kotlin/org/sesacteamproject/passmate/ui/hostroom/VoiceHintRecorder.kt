package org.sesacteamproject.passmate.ui.hostroom

import androidx.compose.runtime.Composable

// PTT 녹음 결과 — 업로드 포맷은 앱=audio/mp4(m4a/AAC) (contracts §힌트).
// ByteArray 보유라 data class 미사용(동등성 비교 대상 아님)
class RecordedVoiceHint(
    val audioBytes: ByteArray,
    val mimeType: String,
    val fileName: String,
    val durationMs: Long
)

// PTT 녹음 제어 — 플랫폼 구현은 expect/actual 뒤로 숨긴다 (규칙 §2)
interface VoiceHintRecorderController {

    // false = 마이크 권한 없음(요청 트리거됨)·시작 실패 — 화면은 안내만 하고 녹음 상태로 가지 않는다
    fun start(): Boolean

    // null = 녹음 실패·너무 짧음(무효 클립)
    fun stop(): RecordedVoiceHint?

    fun cancel()
}

// Android=MediaRecorder(m4a/AAC)+RECORD_AUDIO 런타임 권한, Desktop=미지원(null 반환 시 안내 칩만 표시)
@Composable
expect fun rememberVoiceHintRecorder(): VoiceHintRecorderController?
