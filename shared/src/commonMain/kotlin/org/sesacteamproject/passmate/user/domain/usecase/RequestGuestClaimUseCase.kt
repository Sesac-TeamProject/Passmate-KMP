package org.sesacteamproject.passmate.user.domain.usecase

import org.sesacteamproject.passmate.user.domain.model.PendingGuestClaim

// "가입하고 기록 저장" 탭 시 participantId를 대기 큐에 넣는다 — 로그인 완료 후 CompleteGuestClaimUseCase가 소비
class RequestGuestClaimUseCase(
    private val pendingGuestClaim: PendingGuestClaim
) {
    operator fun invoke(participantId: Long) {
        pendingGuestClaim.request(participantId)
    }
}
