package org.sesacteamproject.passmate.report.data.repository

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.core.model.map
import org.sesacteamproject.passmate.core.network.apiCall
import org.sesacteamproject.passmate.report.data.mapper.toDomain
import org.sesacteamproject.passmate.report.data.remote.ResultRemoteDataSource
import org.sesacteamproject.passmate.report.domain.model.LearningReport
import org.sesacteamproject.passmate.report.domain.model.SessionResult
import org.sesacteamproject.passmate.report.domain.repository.ResultRepository

class ResultRepositoryImpl(
    private val remoteDataSource: ResultRemoteDataSource
) : ResultRepository {

    override suspend fun getSessionResult(roomId: Long): AppResult<SessionResult> {
        return apiCall { remoteDataSource.fetchSessionResult(roomId) }.map { it.toDomain() }
    }

    override suspend fun getLearningReport(roomId: Long): AppResult<LearningReport> {
        return apiCall { remoteDataSource.fetchLearningReport(roomId) }.map { it.toDomain() }
    }
}
