package org.sesacteamproject.passmate.ui.waiting

import org.sesacteamproject.passmate.room.domain.model.Participant

data class WaitingUiState(
    val isLoading: Boolean = true,
    val roomTitle: String = "",
    val pin: String = "",
    val myParticipantId: Long? = null,
    val myNickname: String? = null,
    val participants: List<Participant> = emptyList(),
    val totalCount: Int = 0
)
