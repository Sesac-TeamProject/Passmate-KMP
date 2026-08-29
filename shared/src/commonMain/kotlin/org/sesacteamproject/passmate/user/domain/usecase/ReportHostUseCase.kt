package org.sesacteamproject.passmate.user.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.user.domain.model.ReportReason
import org.sesacteamproject.passmate.user.domain.repository.UserRepository

class ReportHostUseCase(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userId: Long, reason: ReportReason, detail: String?): AppResult<Unit> {
        return userRepository.reportUser(userId, reason, detail)
    }
}
