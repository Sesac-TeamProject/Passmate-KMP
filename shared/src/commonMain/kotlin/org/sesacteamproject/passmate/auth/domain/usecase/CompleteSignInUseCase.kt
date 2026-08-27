package org.sesacteamproject.passmate.auth.domain.usecase

import org.sesacteamproject.passmate.auth.domain.repository.AuthRepository
import org.sesacteamproject.passmate.core.model.AppResult

class CompleteSignInUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(accessToken: String, refreshToken: String): AppResult<Unit> {
        return authRepository.completeSignIn(accessToken, refreshToken)
    }
}
