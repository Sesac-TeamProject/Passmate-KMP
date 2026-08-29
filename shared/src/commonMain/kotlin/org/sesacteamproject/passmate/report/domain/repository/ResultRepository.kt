package org.sesacteamproject.passmate.report.domain.repository

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.report.domain.model.LearningReport
import org.sesacteamproject.passmate.report.domain.model.RoomReport
import org.sesacteamproject.passmate.report.domain.model.SessionResult

interface ResultRepository {

    suspend fun getSessionResult(roomId: Long): AppResult<SessionResult>

    suspend fun getLearningReport(roomId: Long): AppResult<LearningReport>

    // 선생님용 방 리포트 — 세션 전체 통계·학생별 결과 (M-14, FR-031)
    suspend fun getRoomReport(roomId: Long): AppResult<RoomReport>
}
