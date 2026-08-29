package org.sesacteamproject.passmate.auth.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.auth.domain.repository.AuthRepository

// 로그아웃 (M-12-11) — 서버 refresh 무효화는 최선 시도, 로컬 세션 정리는 항상 수행
class SignOutUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): AppResult<Unit> {
        return authRepository.signOut()
    }
}
