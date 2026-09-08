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
    // STOMP가 끊긴 동안 true — 컨테이너가 M-07 연결 끊김 오버레이를 띄운다
    val isDisconnected: Boolean = false
)
