package org.sesacteamproject.passmate.question.domain.model

// 내 문제 세트 1건 (GET /question-sets) — M-13 새 방 만들기 시트의 세트 선택용
data class QuestionSetSummary(
    val setId: Long,
    val title: String,
    val isConfirmed: Boolean,
    val questionCount: Int
)
