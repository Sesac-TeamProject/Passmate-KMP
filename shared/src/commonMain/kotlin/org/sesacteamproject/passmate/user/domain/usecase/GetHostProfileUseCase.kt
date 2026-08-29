package org.sesacteamproject.passmate.user.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.user.domain.model.HostProfile
import org.sesacteamproject.passmate.user.domain.repository.UserRepository

class GetHostProfileUseCase(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userId: Long): AppResult<HostProfile> {
        return userRepository.getHostProfile(userId)
    }
}
