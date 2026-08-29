package org.sesacteamproject.passmate.user.domain.usecase

import org.sesacteamproject.passmate.auth.domain.repository.AuthRepository
import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.core.model.onSuccess
import org.sesacteamproject.passmate.user.domain.repository.UserRepository

// 회원 탈퇴 (M-12-12) — 서버 soft delete 성공 시 로컬 세션도 정리한다.
// 정산 미지급분·진행 중 방이 있으면 서버가 409로 거부(문구는 서버 message 사용)
class DeleteAccountUseCase(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): AppResult<Unit> {
        return userRepository.deleteAccount()
            .onSuccess { authRepository.clearSession() }
    }
}
