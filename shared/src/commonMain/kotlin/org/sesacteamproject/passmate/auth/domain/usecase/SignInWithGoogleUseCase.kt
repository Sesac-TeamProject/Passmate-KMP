package org.sesacteamproject.passmate.auth.domain.usecase

import org.sesacteamproject.passmate.auth.domain.repository.AuthRepository
import org.sesacteamproject.passmate.core.model.AppResult

class SignInWithGoogleUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(idToken: String): AppResult<Unit> {
        return authRepository.signInWithGoogle(idToken)
    }
}
