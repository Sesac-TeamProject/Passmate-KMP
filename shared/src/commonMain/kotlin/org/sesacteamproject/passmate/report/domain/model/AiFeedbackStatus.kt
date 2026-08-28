package org.sesacteamproject.passmate.report.domain.model

// 분석 중/완료/실패/한도 소진(SKIPPED) — SKIPPED는 세션 시작 시 aiAnalysisEnabled=false (FR-062)
enum class AiFeedbackStatus {
    PENDING,
    DONE,
    FAILED,
    SKIPPED,
    NONE;

    companion object {

        fun from(raw: String?): AiFeedbackStatus {
            return when (raw?.uppercase()) {
                "PENDING" -> PENDING
                "DONE" -> DONE
                "FAILED" -> FAILED
                "SKIPPED" -> SKIPPED
                else -> NONE
            }
        }
    }
}
