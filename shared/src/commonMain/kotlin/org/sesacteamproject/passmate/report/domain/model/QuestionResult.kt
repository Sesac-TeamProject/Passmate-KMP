package org.sesacteamproject.passmate.report.domain.model

import org.sesacteamproject.passmate.session.domain.model.QuestionType

// 문항 1개의 결과 — 정오·내 답변·정답/해설·AI 피드백·첨삭 (GET /rooms/{roomId}/results/me)
data class QuestionResult(
    val questionId: Long,
    val questionNo: Int,
    val title: String,
    val type: QuestionType,
    val verdict: AnswerVerdict,
    val myAnswer: String?,
    val correctAnswer: String?,
    val explanation: String?,
    val earnedScore: Double,
    val aiFeedback: AiFeedback?,
    val hostReview: HostReview?
)
