package org.sesacteamproject.passmate.session.data.dto

import kotlinx.serialization.Serializable

// POST .../answers 응답 — 즉시 채점 결과(서술형은 잠정 배점·correct null)
@Serializable
data class SubmitAnswerResponse(
    val correct: Boolean? = null,
    val baseScore: Double = 0.0,
    val speedBonus: Double = 0.0,
    val earnedScore: Double = 0.0,
    val totalScore: Double = 0.0,
    val rank: Int? = null,
    val rankDelta: Int? = null,
    val isProvisional: Boolean = false
)
