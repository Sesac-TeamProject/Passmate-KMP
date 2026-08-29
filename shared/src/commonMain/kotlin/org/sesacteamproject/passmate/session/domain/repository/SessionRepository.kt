package org.sesacteamproject.passmate.session.domain.repository

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.session.domain.model.AnswerResult
import org.sesacteamproject.passmate.session.domain.model.SessionSnapshot
import org.sesacteamproject.passmate.session.domain.model.StartSessionResult
import org.sesacteamproject.passmate.session.domain.model.SubmissionStatus
import org.sesacteamproject.passmate.session.domain.model.VoiceHint

interface SessionRepository {

    suspend fun getSnapshot(roomId: Long): AppResult<SessionSnapshot>

    suspend fun submitAnswer(roomId: Long, questionId: Long, content: String): AppResult<AnswerResult>

    suspend fun getVoiceHints(roomId: Long): AppResult<List<VoiceHint>>

    // ── 호스트 세션 제어 (M-T2 리모컨) — 상태 전이·브로드캐스트는 전부 서버가 한다 ──

    // 세트 미확정이면 409 — aiAnalysisEnabled=false면 이 세션의 서술형 분석은 SKIPPED (FR-062)
    suspend fun startSession(roomId: Long): AppResult<StartSessionResult>

    suspend fun nextQuestion(roomId: Long): AppResult<Unit>

    suspend fun endCurrentQuestion(roomId: Long): AppResult<Unit>

    suspend fun endSession(roomId: Long): AppResult<Unit>

    suspend fun setScreenLock(roomId: Long, locked: Boolean): AppResult<Unit>

    // 현재 문항 제출 현황 — SUBMISSION_UPDATED 수신 시 재조회한다
    suspend fun getSubmissions(roomId: Long): AppResult<SubmissionStatus>
}
