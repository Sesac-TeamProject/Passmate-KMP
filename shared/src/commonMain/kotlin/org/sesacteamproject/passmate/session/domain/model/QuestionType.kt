package org.sesacteamproject.passmate.session.domain.model

enum class QuestionType {
    MULTIPLE_CHOICE,
    OX,
    ESSAY,
    UNKNOWN;

    companion object {

        fun from(raw: String?): QuestionType {
            return when (raw?.uppercase()) {
                // 서버 enum은 MCQ다 (계약 `QuestionRequest.type`: MCQ·OX·ESSAY)
                "MCQ", "MULTIPLE_CHOICE" -> MULTIPLE_CHOICE
                "OX" -> OX
                "ESSAY" -> ESSAY
                else -> UNKNOWN
            }
        }
    }
}
