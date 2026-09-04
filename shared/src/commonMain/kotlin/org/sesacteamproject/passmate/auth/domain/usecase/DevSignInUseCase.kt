package org.sesacteamproject.passmate.auth.domain.usecase

import org.sesacteamproject.passmate.auth.domain.repository.AuthRepository
import org.sesacteamproject.passmate.core.model.AppResult

// 로컬 개발 서버 전용 로그인 — Google 로그인이 붙기 전까지 회원 화면을 확인하는 경로다
class DevSignInUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): AppResult<Unit> {
        return authRepository.devSignIn()
    }
}
