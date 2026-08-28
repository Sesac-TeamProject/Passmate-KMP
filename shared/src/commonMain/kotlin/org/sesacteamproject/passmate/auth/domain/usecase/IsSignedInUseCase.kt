package org.sesacteamproject.passmate.auth.domain.usecase

import org.sesacteamproject.passmate.auth.domain.repository.AuthRepository

class IsSignedInUseCase(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Boolean {
        return authRepository.isSignedIn()
    }
}
