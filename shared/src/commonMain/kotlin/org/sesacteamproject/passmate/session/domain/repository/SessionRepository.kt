package org.sesacteamproject.passmate.session.domain.repository

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.session.domain.model.AnswerResult
import org.sesacteamproject.passmate.session.domain.model.SessionSnapshot
import org.sesacteamproject.passmate.session.domain.model.VoiceHint

interface SessionRepository {

    suspend fun getSnapshot(roomId: Long): AppResult<SessionSnapshot>

    suspend fun submitAnswer(roomId: Long, questionId: Long, content: String): AppResult<AnswerResult>

    suspend fun getVoiceHints(roomId: Long): AppResult<List<VoiceHint>>
}
