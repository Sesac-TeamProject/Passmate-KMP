package org.sesacteamproject.passmate.session.domain.model

// 현재 문항 제출 현황 (GET /rooms/{roomId}/session/current/submissions) — M-T2 리모컨 렌더링용
data class SubmissionStatus(
    val questionNo: Int,
    val submittedCount: Int,
    val totalCount: Int,
    val accuracyPercent: Int?,
    val choices: List<ChoiceCount>,
    val participants: List<SubmissionParticipant>
)

data class ChoiceCount(
    val label: String,
    val count: Int
)

data class SubmissionParticipant(
    val participantId: Long,
    val nickname: String,
    val avatarId: Int?,
    val submitted: Boolean
)

// 세션 시작 결과 — aiAnalysisEnabled=false면 이 세션의 서술형 AI 분석은 SKIPPED (FR-062)
data class StartSessionResult(
    val aiAnalysisEnabled: Boolean
)
