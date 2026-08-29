package org.sesacteamproject.passmate.session.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.session.domain.repository.SessionRepository

class EndSessionUseCase(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(roomId: Long): AppResult<Unit> {
        return sessionRepository.endSession(roomId)
    }
}
