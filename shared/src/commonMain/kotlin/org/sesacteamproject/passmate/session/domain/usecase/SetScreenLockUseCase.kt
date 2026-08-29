package org.sesacteamproject.passmate.session.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.session.domain.repository.SessionRepository

class SetScreenLockUseCase(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(roomId: Long, locked: Boolean): AppResult<Unit> {
        return sessionRepository.setScreenLock(roomId, locked)
    }
}
