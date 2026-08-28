package org.sesacteamproject.passmate.ui.play

import org.sesacteamproject.passmate.session.domain.model.AnswerResult
import org.sesacteamproject.passmate.session.domain.model.RankEntry
import org.sesacteamproject.passmate.session.domain.model.SessionQuestion
import org.sesacteamproject.passmate.session.domain.model.VoiceHint

data class PlayUiState(
    val isLoading: Boolean = true,
    val phase: Phase = Phase.IDLE,
    val questionCount: Int = 0,
    val question: SessionQuestion? = null,
    val selectedChoiceIndex: Int? = null,
    val essayAnswer: String = "",
    val remainingSeconds: Int = 0,
    val isSubmitting: Boolean = false,
    val hasSubmitted: Boolean = false,
    val myAnswerResult: AnswerResult? = null,
    val reveal: Reveal? = null,
    val totalScore: Double = 0.0,
    val myCorrectCount: Int = 0,
    val rank: Int? = null,
    val ranking: List<RankEntry> = emptyList(),
    val finalRanking: List<RankEntry> = emptyList(),
    val correctCount: Int = 0,
    val myParticipantId: Long? = null,
    val myNickname: String? = null,
    val isLocked: Boolean = false,
    val activeVoiceHint: VoiceHint? = null
) {

    // 화면 단계 — 전환은 전부 서버 이벤트·스냅샷으로만 일어난다 (규칙 §2-1-2)
    enum class Phase {
        IDLE,
        QUESTION,
        FINISHED
    }

    // QUESTION_ENDED 정답 공개 페이로드 (정답은 이 이벤트에서만 온다 — 규칙 §13)
    data class Reveal(
        val answer: String?,
        val explanation: String?,
        val correctAnswererCount: Int
    )
}
