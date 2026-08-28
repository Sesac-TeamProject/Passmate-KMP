package org.sesacteamproject.passmate.user.domain.repository

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.user.domain.model.MyPage

interface UserRepository {

    // cursor가 null이면 첫 페이지(요약·진행중 포함), 있으면 목록 다음 페이지
    suspend fun getMyPage(cursor: String?): AppResult<MyPage>

    // 게스트 기록 계정 연동 — 가입 후 7일 내, 경과 시 410 RECORD_PURGED (FR-036)
    suspend fun claimGuestRecord(participantId: Long): AppResult<Unit>
}
