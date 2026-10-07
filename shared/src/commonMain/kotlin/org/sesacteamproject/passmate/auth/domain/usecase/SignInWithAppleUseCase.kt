package org.sesacteamproject.passmate.auth.domain.usecase

import org.sesacteamproject.passmate.auth.domain.repository.AuthRepository
import org.sesacteamproject.passmate.core.model.AppResult

class SignInWithAppleUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(idToken: String, authorizationCode: String?, name: String?): AppResult<Unit> {
        return authRepository.signInWithApple(idToken, authorizationCode, name)
    }
}
