package org.sesacteamproject.passmate.session.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.session.domain.repository.SessionRepository

// 현재 문항 수동 마감 (M-T2 "바로 마감") — 마감·채점·랭킹 갱신은 서버가 한다
class EndCurrentQuestionUseCase(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(roomId: Long): AppResult<Unit> {
        return sessionRepository.endCurrentQuestion(roomId)
    }
}
