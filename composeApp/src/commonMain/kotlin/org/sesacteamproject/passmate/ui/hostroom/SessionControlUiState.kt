package org.sesacteamproject.passmate.ui.hostroom

import org.sesacteamproject.passmate.room.domain.model.RoomStatus
import org.sesacteamproject.passmate.session.domain.model.SessionQuestion
import org.sesacteamproject.passmate.session.domain.model.SubmissionStatus

data class SessionControlUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val roomTitle: String = "",
    val pin: String = "",
    val status: RoomStatus = RoomStatus.WAITING,
    val participantCount: Int = 0,
    val questionCount: Int? = null,
    val question: SessionQuestion? = null,
    // 서버 endsAt 기반 렌더링 전용 초 카운트 — 마감 판정은 서버가 한다 (규칙 §1·§5)
    val remainingSec: Int = 0,
    val isQuestionClosed: Boolean = false,
    val submissions: SubmissionStatus? = null,
    val isLocked: Boolean = false,
    val isProjectorConnected: Boolean = false,
    // 제어 요청 in-flight — 중복 호출 방지 (규칙 §9)
    val isControlling: Boolean = false
)
