package org.sesacteamproject.passmate.user.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.user.domain.model.Badge
import org.sesacteamproject.passmate.user.domain.repository.UserRepository

class GetMyBadgesUseCase(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): AppResult<List<Badge>> {
        return userRepository.getMyBadges()
    }
}
