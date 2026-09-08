package org.sesacteamproject.passmate.testing

import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.session.domain.model.AnswerResult
import org.sesacteamproject.passmate.session.domain.model.SessionSnapshot
import org.sesacteamproject.passmate.session.domain.model.StartSessionResult
import org.sesacteamproject.passmate.session.domain.model.SubmissionStatus
import org.sesacteamproject.passmate.session.domain.model.VoiceHint
import org.sesacteamproject.passmate.session.domain.repository.SessionRepository

// 스냅샷·제출·힌트 전부 실패를 돌려주는 최소 가짜 — 연결 상태처럼 응답 내용과 무관한 ViewModel 동작 검증용.
// 제출 실패는 서버 code마다 화면 문구가 갈리므로(규칙 §10) 테스트가 오류를 갈아 끼울 수 있게 열어 둔다
class FakeSessionRepository : SessionRepository {

    var submitError: AppError = AppError.Unknown()

    override suspend fun getSnapshot(roomId: Long): AppResult<SessionSnapshot> {
        return AppResult.Failure(AppError.Unknown())
    }

    override suspend fun submitAnswer(roomId: Long, questionId: Long, content: String): AppResult<AnswerResult> {
        return AppResult.Failure(submitError)
    }

    override suspend fun getVoiceHints(roomId: Long): AppResult<List<VoiceHint>> {
        return AppResult.Failure(AppError.Unknown())
    }

    override suspend fun startSession(roomId: Long): AppResult<StartSessionResult> {
        return AppResult.Failure(AppError.Unknown())
    }

    override suspend fun nextQuestion(roomId: Long): AppResult<Unit> {
        return AppResult.Failure(AppError.Unknown())
    }

    override suspend fun endCurrentQuestion(roomId: Long): AppResult<Unit> {
        return AppResult.Failure(AppError.Unknown())
    }

    override suspend fun endSession(roomId: Long): AppResult<Unit> {
        return AppResult.Failure(AppError.Unknown())
    }

    override suspend fun setScreenLock(roomId: Long, locked: Boolean): AppResult<Unit> {
        return AppResult.Failure(AppError.Unknown())
    }

    override suspend fun getSubmissions(roomId: Long): AppResult<SubmissionStatus> {
        return AppResult.Failure(AppError.Unknown())
    }

    override suspend fun publishVoiceHint(
        roomId: Long,
        audioBytes: ByteArray,
        mimeType: String,
        fileName: String,
        durationMs: Long
    ): AppResult<VoiceHint> {
        return AppResult.Failure(AppError.Unknown())
    }
}
