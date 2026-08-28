package org.sesacteamproject.passmate.session.data.repository

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.core.model.map
import org.sesacteamproject.passmate.core.network.apiCall
import org.sesacteamproject.passmate.session.data.dto.SubmitAnswerRequest
import org.sesacteamproject.passmate.session.data.mapper.toDomain
import org.sesacteamproject.passmate.session.data.remote.SessionRemoteDataSource
import org.sesacteamproject.passmate.session.domain.model.AnswerResult
import org.sesacteamproject.passmate.session.domain.model.SessionSnapshot
import org.sesacteamproject.passmate.session.domain.repository.SessionRepository

class SessionRepositoryImpl(
    private val remoteDataSource: SessionRemoteDataSource
) : SessionRepository {

    override suspend fun getSnapshot(roomId: Long): AppResult<SessionSnapshot> {
        return apiCall { remoteDataSource.fetchSnapshot(roomId) }.map { it.toDomain() }
    }

    override suspend fun submitAnswer(roomId: Long, questionId: Long, content: String): AppResult<AnswerResult> {
        val request = SubmitAnswerRequest(content = content)

        return apiCall { remoteDataSource.submitAnswer(roomId, questionId, request) }.map { it.toDomain() }
    }
}
