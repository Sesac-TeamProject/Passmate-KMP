package org.sesacteamproject.passmate.user.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.user.domain.model.PendingGuestClaim
import org.sesacteamproject.passmate.user.domain.repository.UserRepository

// 로그인 완료 후 호출 — 대기 중인 게스트 기록이 있으면 연동한다. 없으면 null 반환
class CompleteGuestClaimUseCase(
    private val pendingGuestClaim: PendingGuestClaim,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): AppResult<Unit>? {
        val participantId = pendingGuestClaim.consume() ?: return null

        return userRepository.claimGuestRecord(participantId)
    }
}
