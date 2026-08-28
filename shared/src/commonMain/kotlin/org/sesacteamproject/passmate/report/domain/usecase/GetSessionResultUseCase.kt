package org.sesacteamproject.passmate.report.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.report.domain.model.SessionResult
import org.sesacteamproject.passmate.report.domain.repository.ResultRepository

class GetSessionResultUseCase(
    private val resultRepository: ResultRepository
) {
    suspend operator fun invoke(roomId: Long): AppResult<SessionResult> {
        return resultRepository.getSessionResult(roomId)
    }
}
