package org.sesacteamproject.passmate.session.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.session.domain.model.AnswerResult
import org.sesacteamproject.passmate.session.domain.repository.SessionRepository

class SubmitAnswerUseCase(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(roomId: Long, questionId: Long, content: String): AppResult<AnswerResult> {
        return sessionRepository.submitAnswer(roomId, questionId, content)
    }
}
