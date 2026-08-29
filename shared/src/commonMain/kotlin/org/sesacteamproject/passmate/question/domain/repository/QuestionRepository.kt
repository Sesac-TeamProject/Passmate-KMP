package org.sesacteamproject.passmate.question.domain.repository

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.core.model.PagedResult
import org.sesacteamproject.passmate.question.domain.model.QuestionSetSummary

interface QuestionRepository {

    // 내 문제 세트 목록 — 모바일은 확정(CONFIRMED) 세트만 사용한다 (M-13 시트, FR-014)
    suspend fun getMySets(confirmedOnly: Boolean, cursor: String?): AppResult<PagedResult<QuestionSetSummary>>
}
