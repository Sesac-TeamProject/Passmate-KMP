package org.sesacteamproject.passmate.session.data.dto

import kotlinx.serialization.Serializable

// GET /rooms/{roomId}/session/current/submissions 응답 — contracts §Session과 1:1
@Serializable
data class SubmissionsResponse(
    val questionNo: Int = 0,
    val submittedCount: Int = 0,
    val totalCount: Int = 0,
    val accuracyPercent: Int? = null,
    val choices: List<ChoiceDto>? = null,
    val participants: List<ParticipantDto> = emptyList()
) {

    @Serializable
    data class ChoiceDto(
        val label: String = "",
        val count: Int = 0
    )

    @Serializable
    data class ParticipantDto(
        val participantId: Long = 0,
        val nickname: String = "",
        val avatarId: Int? = null,
        val submitted: Boolean = false
    )
}

// POST /rooms/{roomId}/session/start 응답 — 서술형 무료 분석 한도 판별 (FR-062)
@Serializable
data class StartSessionResponse(
    val aiAnalysisEnabled: Boolean = true
)

// PUT /rooms/{roomId}/session/lock 요청 — 학생 화면 잠금/해제 (M-T2)
@Serializable
data class ScreenLockRequest(
    val locked: Boolean
)
