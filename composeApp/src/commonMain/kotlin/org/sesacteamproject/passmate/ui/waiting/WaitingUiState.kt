package org.sesacteamproject.passmate.ui.waiting

import org.sesacteamproject.passmate.room.domain.model.Participant

data class WaitingUiState(
    val isLoading: Boolean = true,
    val roomTitle: String = "",
    val pin: String = "",
    val myParticipantId: Long? = null,
    val myNickname: String? = null,
    val participants: List<Participant> = emptyList(),
    val totalCount: Int = 0,
    // 세션 종료는 상태로도 남긴다 — 대기실이 백스택에 있는 동안 발행한 event는 아무도 받지 못한다 (규칙 §7 replay=0)
    val isSessionFinished: Boolean = false
)
