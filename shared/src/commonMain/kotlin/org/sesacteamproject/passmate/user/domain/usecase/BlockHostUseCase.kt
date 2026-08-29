package org.sesacteamproject.passmate.user.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.user.domain.repository.UserRepository

class BlockHostUseCase(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userId: Long): AppResult<Unit> {
        return userRepository.blockUser(userId)
    }
}
