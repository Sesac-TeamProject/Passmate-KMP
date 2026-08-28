package org.sesacteamproject.passmate.report.domain.model

// 문항별 정오 표시 — 서술형은 채점 전/AI 분석 상태로 구분 (M-06 문항 리스트 칩)
enum class AnswerVerdict {
    CORRECT,
    WRONG,
    AI_ANALYZED,
    AI_PENDING,
    UNGRADED;

    companion object {

        fun from(raw: String?): AnswerVerdict {
            return when (raw?.uppercase()) {
                "CORRECT" -> CORRECT
                "WRONG", "INCORRECT" -> WRONG
                "AI_ANALYZED", "ANALYZED" -> AI_ANALYZED
                "AI_PENDING", "PENDING" -> AI_PENDING
                else -> UNGRADED
            }
        }
    }
}
