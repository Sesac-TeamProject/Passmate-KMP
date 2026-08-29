package org.sesacteamproject.passmate.session.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.session.domain.model.VoiceHint
import org.sesacteamproject.passmate.session.domain.repository.SessionRepository

// PTT 힌트 송출 (M-T2 "길게 눌러 힌트 말하기") — 브로드캐스트·수신 SLA는 서버 몫 (FR-039, SC-006)
class PublishVoiceHintUseCase(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(
        roomId: Long,
        audioBytes: ByteArray,
        mimeType: String,
        fileName: String,
        durationMs: Long
    ): AppResult<VoiceHint> {
        return sessionRepository.publishVoiceHint(roomId, audioBytes, mimeType, fileName, durationMs)
    }
}
