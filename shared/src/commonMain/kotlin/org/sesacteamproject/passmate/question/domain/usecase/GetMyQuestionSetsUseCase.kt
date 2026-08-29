package org.sesacteamproject.passmate.question.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.core.model.PagedResult
import org.sesacteamproject.passmate.question.domain.model.QuestionSetSummary
import org.sesacteamproject.passmate.question.domain.repository.QuestionRepository

class GetMyQuestionSetsUseCase(
    private val questionRepository: QuestionRepository
) {
    suspend operator fun invoke(confirmedOnly: Boolean, cursor: String?): AppResult<PagedResult<QuestionSetSummary>> {
        return questionRepository.getMySets(confirmedOnly, cursor)
    }
}
