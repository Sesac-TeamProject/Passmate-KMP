package org.sesacteamproject.passmate.testing

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.core.model.PagedResult
import org.sesacteamproject.passmate.question.domain.model.QuestionSetSummary
import org.sesacteamproject.passmate.question.domain.repository.QuestionRepository

class FakeQuestionRepository(
    var sets: List<QuestionSetSummary> = emptyList()
) : QuestionRepository {

    override suspend fun getMySets(confirmedOnly: Boolean, cursor: String?): AppResult<PagedResult<QuestionSetSummary>> {
        return AppResult.Success(PagedResult(items = sets, nextCursor = null, hasNext = false))
    }
}
