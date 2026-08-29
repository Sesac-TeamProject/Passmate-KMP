package org.sesacteamproject.passmate.user.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.user.domain.model.MyGrade
import org.sesacteamproject.passmate.user.domain.repository.UserRepository

class GetMyGradeUseCase(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): AppResult<MyGrade> {
        return userRepository.getMyGrade()
    }
}
