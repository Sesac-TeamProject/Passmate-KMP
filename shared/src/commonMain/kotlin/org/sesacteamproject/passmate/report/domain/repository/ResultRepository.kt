package org.sesacteamproject.passmate.report.domain.repository

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.report.domain.model.LearningReport
import org.sesacteamproject.passmate.report.domain.model.SessionResult

interface ResultRepository {

    suspend fun getSessionResult(roomId: Long): AppResult<SessionResult>

    suspend fun getLearningReport(roomId: Long): AppResult<LearningReport>
}
