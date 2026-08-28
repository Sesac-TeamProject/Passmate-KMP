package org.sesacteamproject.passmate.session.domain.model

// HINT_PUBLISHED 이벤트·힌트 목록 조회 공용 (FR-039~041)
data class VoiceHint(
    val hintId: Long,
    val questionNo: Int,
    val clipUrl: String,
    val durationMs: Long
)
