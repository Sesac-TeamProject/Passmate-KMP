package org.sesacteamproject.passmate.session.domain.model

enum class QuestionType {
    MULTIPLE_CHOICE,
    OX,
    ESSAY,
    UNKNOWN;

    // 화면 표시 이름 — Compose·SwiftUI가 각자 적으면 네 곳으로 갈라지므로 여기 한 곳에만 둔다.
    // 표시 문자열을 도메인에 두는 것은 OX_CHOICES(SessionQuestion)와 같은 선례를 따른다 (규칙 §2)
    val displayLabel: String
        get() = when (this) {
            MULTIPLE_CHOICE -> "객관식"
            OX -> "OX"
            ESSAY -> "서술형"
            UNKNOWN -> "문항"
        }

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
