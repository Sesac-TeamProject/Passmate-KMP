package org.sesacteamproject.passmate.session.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.session.domain.model.StartSessionResult
import org.sesacteamproject.passmate.session.domain.repository.SessionRepository

// 세션 시작 (M-T2) — 세트 미확정 409, 상태 전이·브로드캐스트는 서버가 한다
class StartSessionUseCase(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(roomId: Long): AppResult<StartSessionResult> {
        return sessionRepository.startSession(roomId)
    }
}
