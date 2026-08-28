package org.sesacteamproject.passmate.session.domain.model

// 진행 중 문항 — 정답은 절대 포함하지 않는다(정답은 QUESTION_ENDED에서만, 규칙 §13)
data class SessionQuestion(
    val questionId: Long,
    val questionNo: Int,
    val type: QuestionType,
    val body: String,
    val choices: List<String>,
    val points: Int,
    val timeLimitSec: Int,
    val endsAt: String,
    val isClosed: Boolean
)
