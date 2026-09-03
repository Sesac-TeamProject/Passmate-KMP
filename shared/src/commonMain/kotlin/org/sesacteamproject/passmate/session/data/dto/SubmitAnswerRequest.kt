package org.sesacteamproject.passmate.session.data.dto

import kotlinx.serialization.Serializable

// POST /rooms/{roomId}/session/questions/{questionId}/answers 요청 — 계약 `AnswerSubmitRequest`.
// submitted — 객관식: 선택한 보기 원문, OX: "O"|"X", 서술형: 자유 텍스트
@Serializable
data class SubmitAnswerRequest(
    val submitted: String
)
