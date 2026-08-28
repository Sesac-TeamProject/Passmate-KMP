package org.sesacteamproject.passmate.session.domain.model

enum class QuestionType {
    MULTIPLE_CHOICE,
    OX,
    ESSAY,
    UNKNOWN;

    companion object {

        fun from(raw: String?): QuestionType {
            return when (raw?.uppercase()) {
                "MULTIPLE_CHOICE" -> MULTIPLE_CHOICE
                "OX" -> OX
                "ESSAY" -> ESSAY
                else -> UNKNOWN
            }
        }
    }
}
