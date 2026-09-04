package org.sesacteamproject.passmate.auth.domain.usecase

import org.sesacteamproject.passmate.auth.domain.repository.AuthRepository

// 개발용 로그인 진입점 노출 여부 — 로컬 개발 서버에 붙어 있을 때만 true
class IsDevSignInAvailableUseCase(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Boolean {
        return authRepository.isDevSignInAvailable()
    }
}
