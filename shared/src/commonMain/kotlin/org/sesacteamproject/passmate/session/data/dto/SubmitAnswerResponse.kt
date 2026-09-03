package org.sesacteamproject.passmate.session.data.dto

import kotlinx.serialization.Serializable

// POST .../answers 응답 — 계약 `AnswerResponse`.
// 누적 점수·순위는 이 응답에 없고 RANKING_UPDATED 이벤트로 갱신된다.
@Serializable
data class SubmitAnswerResponse(
    val answerId: Long = 0,
    val sessionQuestionId: Long = 0,
    val isCorrect: Boolean? = null,
    val baseScore: Int = 0,
    val speedBonus: Int = 0,
    val score: Int = 0,
    val submittedAt: String? = null
)
