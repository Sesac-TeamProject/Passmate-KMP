package org.sesacteamproject.passmate.auth.domain.usecase

import org.sesacteamproject.passmate.auth.domain.repository.AuthRepository

class BuildGoogleSignInUrlUseCase(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): String {
        return authRepository.googleSignInUrl()
    }
}
