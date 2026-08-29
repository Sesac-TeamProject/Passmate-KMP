package org.sesacteamproject.passmate.session.data.repository

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.core.model.map
import org.sesacteamproject.passmate.core.network.apiCall
import org.sesacteamproject.passmate.session.data.dto.ScreenLockRequest
import org.sesacteamproject.passmate.session.data.dto.SubmitAnswerRequest
import org.sesacteamproject.passmate.session.data.mapper.toDomain
import org.sesacteamproject.passmate.session.data.remote.SessionRemoteDataSource
import org.sesacteamproject.passmate.session.domain.model.AnswerResult
import org.sesacteamproject.passmate.session.domain.model.SessionSnapshot
import org.sesacteamproject.passmate.session.domain.model.StartSessionResult
import org.sesacteamproject.passmate.session.domain.model.SubmissionStatus
import org.sesacteamproject.passmate.session.domain.model.VoiceHint
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

    override suspend fun getVoiceHints(roomId: Long): AppResult<List<VoiceHint>> {
        return apiCall { remoteDataSource.fetchVoiceHints(roomId) }
            .map { response -> response.hints.map { it.toDomain() } }
    }

    override suspend fun startSession(roomId: Long): AppResult<StartSessionResult> {
        return apiCall { remoteDataSource.startSession(roomId) }
            .map { StartSessionResult(aiAnalysisEnabled = it.aiAnalysisEnabled) }
    }

    override suspend fun nextQuestion(roomId: Long): AppResult<Unit> {
        return apiCall { remoteDataSource.nextQuestion(roomId) }
    }

    override suspend fun endCurrentQuestion(roomId: Long): AppResult<Unit> {
        return apiCall { remoteDataSource.endCurrentQuestion(roomId) }
    }

    override suspend fun endSession(roomId: Long): AppResult<Unit> {
        return apiCall { remoteDataSource.endSession(roomId) }
    }

    override suspend fun setScreenLock(roomId: Long, locked: Boolean): AppResult<Unit> {
        return apiCall { remoteDataSource.setScreenLock(roomId, ScreenLockRequest(locked)) }
    }

    override suspend fun getSubmissions(roomId: Long): AppResult<SubmissionStatus> {
        return apiCall { remoteDataSource.fetchSubmissions(roomId) }.map { it.toDomain() }
    }

    override suspend fun publishVoiceHint(
        roomId: Long,
        audioBytes: ByteArray,
        mimeType: String,
        fileName: String,
        durationMs: Long
    ): AppResult<VoiceHint> {
        return apiCall {
            remoteDataSource.publishHint(roomId, audioBytes, mimeType, fileName, durationMs)
        }.map { it.toDomain() }
    }
}
