package org.sesacteamproject.passmate.session.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.session.domain.model.SubmissionStatus
import org.sesacteamproject.passmate.session.domain.repository.SessionRepository

class GetSubmissionsUseCase(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(roomId: Long): AppResult<SubmissionStatus> {
        return sessionRepository.getSubmissions(roomId)
    }
}
